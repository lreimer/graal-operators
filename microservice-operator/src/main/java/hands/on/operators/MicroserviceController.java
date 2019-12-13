package hands.on.operators;

import hands.on.operators.crd.Microservice;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Controller and reconciliation loop for this operator.
 * <br>
 * TODO maybe watch for changes on the created resources and resync
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
        // reconcile all the managed K8s resources
        // TODO maybe add ConfigMap support here later
        reconcileDeployment(microservice);
        reconcileService(microservice);
    }

    private void reconcileDeployment(Microservice microservice) {
        Optional<Deployment> existing = getDeployment(microservice);
        Deployment deployment = existing.orElseGet(() -> createDeployment(microservice));

        // now update everything that might have changed
        Map<String, String> labels = microservice.getMetadata().getLabels();
        deployment.getMetadata().setLabels(labels);
        deployment.getSpec().getSelector().setMatchLabels(labels);
        deployment.getSpec().getTemplate().getMetadata().setLabels(labels);

        deployment.getSpec().setReplicas(microservice.getSpec().getReplicas());
        for (Container c : deployment.getSpec().getTemplate().getSpec().getContainers()) {
            if (Objects.equals(c.getName(), microservice.getMetadata().getName())) {
                c.setImage(microservice.getSpec().getImage());
                c.setPorts(getContainerPorts(microservice));
            }
        }

        client.apps().deployments().inNamespace(namespace).createOrReplace(deployment);
    }

    private void reconcileService(Microservice microservice) {
        Optional<Service> existing = getService(microservice);
        Service service = existing.orElseGet(() -> createService(microservice));

        service.getSpec().setSelector(microservice.getMetadata().getLabels());
        service.getSpec().setType(microservice.getSpec().getServiceType());

        List<ContainerPort> ports = getContainerPorts(microservice);
        List<ServicePort> servicePorts = getServicePorts(ports);
        service.getSpec().setPorts(servicePorts);

        client.services().inNamespace(namespace).createOrReplace(service);
    }

    private Optional<Deployment> getDeployment(Microservice microservice) {
        String name = getDeploymentName(microservice);
        LOGGER.debug("Getting Deployment with name {}", name);

        RollableScalableResource<Deployment, DoneableDeployment> resource = client.apps().deployments().inNamespace(namespace).withName(name);
        return Optional.ofNullable(resource.get());
    }

    private Optional<Service> getService(Microservice microservice) {
        String name = getServiceName(microservice);
        LOGGER.debug("Getting Service with name {}", name);

        ServiceResource<Service, DoneableService> resource = client.services().inNamespace(namespace).withName(name);
        return Optional.ofNullable(resource.get());
    }

    private String getDeploymentName(Microservice microservice) {
        return microservice.getMetadata().getName() + "-deployment";
    }

    private String getServiceName(Microservice microservice) {
        return microservice.getMetadata().getName() + "-service";
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
                .withNewSpec()
                .withNewSelector()
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .endMetadata()
                .withNewSpec()
                .withContainers(createContainer(microservice))
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private List<ContainerPort> getContainerPorts(Microservice microservice) {
        List<ContainerPort> ports = microservice.getSpec().getPorts();
        if (ports.isEmpty()) {
            ports.add(new ContainerPort(8080, null, null, "http-8080", "TCP"));
        }
        return ports;
    }

    private Container createContainer(Microservice microservice) {
        return new ContainerBuilder()
                .withName(microservice.getMetadata().getName())
                .build();
    }

    private Service createService(Microservice microservice) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(getServiceName(microservice))
                .withNamespace(microservice.getMetadata().getNamespace())
                .addNewOwnerReference()
                .withController(true)
                .withKind("Microservice")
                .withApiVersion("operators.on.hands/v1alpha1")
                .withName(microservice.getMetadata().getName())
                .withNewUid(microservice.getMetadata().getUid())
                .endOwnerReference()
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();
    }

    private List<ServicePort> getServicePorts(List<ContainerPort> ports) {
        List<ServicePort> servicePorts = new ArrayList<>();
        for (ContainerPort cp : ports) {
            servicePorts.add(new ServicePort(cp.getName(), null, cp.getContainerPort(), cp.getProtocol(), new IntOrString(cp.getContainerPort())));
        }
        return servicePorts;
    }
}
