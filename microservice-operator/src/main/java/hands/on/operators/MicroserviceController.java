package hands.on.operators;

import hands.on.operators.crd.Microservice;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Controller and reconciliation loop for this operator.
 */
public class MicroserviceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceController.class);

    private final KubernetesClient client;
    private final SharedIndexInformer<Microservice> microserviceInformer;
    private final BlockingQueue<Microservice> workqueue;
    private final String namespace;

    public MicroserviceController(KubernetesClient client, SharedIndexInformer<Microservice> microserviceInformer, String namespace) {
        this.client = client;
        this.microserviceInformer = microserviceInformer;
        this.workqueue = new ArrayBlockingQueue<>(8);
        this.namespace = namespace;
    }

    /**
     * Register for Microservice change events.
     */
    public void create() {
        LOGGER.info("Creating MicroserviceController ...");

        microserviceInformer.addEventHandler(new ResourceEventHandler<Microservice>() {
            @Override
            public void onAdd(Microservice microservice) {
                enqueue(microservice);
            }

            @Override
            public void onUpdate(Microservice oldMicroservice, Microservice newMicroservice) {
                String oldResourceVersion = oldMicroservice.getMetadata().getResourceVersion();
                String newResourceVersion = newMicroservice.getMetadata().getResourceVersion();
                if (Objects.equals(oldResourceVersion, newResourceVersion)) {
                    return;
                }

                enqueue(newMicroservice);
            }

            @Override
            public void onDelete(Microservice obj, boolean deletedFinalStateUnknown) {
                // we rely on the K8s GC mechanism to delete associated Secrets
            }
        });
    }

    public void run() {
        LOGGER.info("Starting MicroserviceController ...");
        while (!microserviceInformer.hasSynced()) ;

        while (true) {
            try {
                LOGGER.debug("Trying to fetch next Microservice from work queue ...");
                if (workqueue.isEmpty()) {
                    LOGGER.debug("Work queue is empty.");
                }

                Microservice microservice = workqueue.take();
                reconcile(microservice);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("MicroserviceController interrupted ...", e);
            }
        }
    }

    private void enqueue(Microservice microservice) {
        String name = microservice.getMetadata().getName();
        LOGGER.info("Going to enqueue Microservice {} to reconcile.", name);
        workqueue.add(microservice);
    }

    private void reconcile(Microservice microservice) {
        Optional<Deployment> existing = getDeployment(microservice);
        Deployment deployment = existing.orElseGet(() -> createDeployment(microservice));

        client.apps().deployments().inNamespace(namespace).createOrReplace(deployment);
    }

    private Optional<Deployment> getDeployment(Microservice microservice) {
        String name = getDeploymentName(microservice);
        LOGGER.debug("Getting Deployment with name {}", name);

        RollableScalableResource<Deployment, DoneableDeployment> resource = client.apps().deployments().inNamespace(namespace).withName(name);
        return Optional.ofNullable(resource.get());
    }

    private String getDeploymentName(Microservice microservice) {
        return microservice.getMetadata().getName() + "-deployment";
    }

    private Deployment createDeployment(Microservice microservice) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(getDeploymentName(microservice))
                .withNamespace(microservice.getMetadata().getNamespace())
                .addNewOwnerReference()
                .withController(true)
                .withKind("Microservice")
                .withApiVersion("operators.on.hands/v1alpha1")
                .withName(microservice.getMetadata().getName())
                .withNewUid(microservice.getMetadata().getUid())
                .endOwnerReference()
                .endMetadata()
                .build();
    }
}
