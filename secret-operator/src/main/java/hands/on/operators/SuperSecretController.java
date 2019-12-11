package hands.on.operators;

import hands.on.operators.crd.DoneableSuperSecret;
import hands.on.operators.crd.SuperSecret;
import hands.on.operators.crd.SuperSecretList;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
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
public class SuperSecretController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperSecretController.class);

    private final KubernetesClient client;
    private final MixedOperation<SuperSecret, SuperSecretList, DoneableSuperSecret, Resource<SuperSecret, DoneableSuperSecret>> superSecretClient;
    private final SharedIndexInformer<SuperSecret> superSecretInformer;
    private final BlockingQueue<SuperSecret> workqueue;
    private final String namespace;

    public SuperSecretController(KubernetesClient client, MixedOperation<SuperSecret, SuperSecretList, DoneableSuperSecret, Resource<SuperSecret, DoneableSuperSecret>> superSecretClient, SharedIndexInformer<SuperSecret> superSecretInformer, String namespace) {
        this.client = client;
        this.superSecretClient = superSecretClient;
        this.superSecretInformer = superSecretInformer;
        this.workqueue = new ArrayBlockingQueue<>(8);
        this.namespace = namespace;
    }

    /**
     * Register for SuperSecret change events.
     */
    public void create() {
        LOGGER.info("Creating SuperSecretController ...");
        superSecretInformer.addEventHandler(new ResourceEventHandler<SuperSecret>() {
            @Override
            public void onAdd(SuperSecret superSecret) {
                enqueueSuperSecret(superSecret);
            }

            @Override
            public void onUpdate(SuperSecret oldSuperSecret, SuperSecret newSuperSecret) {
                String oldResourceVersion = oldSuperSecret.getMetadata().getResourceVersion();
                String newResourceVersion = newSuperSecret.getMetadata().getResourceVersion();
                if (Objects.equals(oldResourceVersion, newResourceVersion)) {
                    return;
                }

                enqueueSuperSecret(newSuperSecret);
            }

            @Override
            public void onDelete(SuperSecret obj, boolean deletedFinalStateUnknown) {
                // we rely on the K8s GC mechanism to delete associated Secrets
            }
        });
    }

    public void run() {
        LOGGER.info("Starting SuperSecretController ...");
        while (!superSecretInformer.hasSynced()) ;

        while (true) {
            try {
                LOGGER.debug("Trying to fetch next SuperSecret from work queue ...");
                if (workqueue.isEmpty()) {
                    LOGGER.debug("Work queue is empty.");
                }

                SuperSecret superSecret = workqueue.take();
                reconcile(superSecret);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("SuperSecretController interrupted ...", e);
            }
        }
    }

    private void enqueueSuperSecret(SuperSecret superSecret) {
        String name = superSecret.getMetadata().getName();
        LOGGER.info("Going to enqueue SuperSecret {} to reconcile.", name);
        workqueue.add(superSecret);
    }

    private void reconcile(SuperSecret superSecret) {
        Optional<Secret> existing = getSecret(superSecret);
        Secret secret = existing.orElseGet(() -> createSecret(superSecret));

        // TODO set decrypted values
        secret.getData().putAll(superSecret.getSpec().getSecretData());
        client.secrets().inNamespace(namespace).createOrReplace(secret);
    }

    private Optional<Secret> getSecret(SuperSecret superSecret) {
        String name = superSecret.getMetadata().getName();
        LOGGER.debug("Getting secret with name {}", name);

        Resource<Secret, DoneableSecret> secretResource = client.secrets().inNamespace(namespace).withName(name);
        return Optional.ofNullable(secretResource.get());
    }

    private Secret createSecret(SuperSecret superSecret) {
        return new SecretBuilder()
                .withNewMetadata()
                .withGenerateName(superSecret.getMetadata().getName())
                .withNamespace(superSecret.getMetadata().getNamespace())
                .addNewOwnerReference()
                .withController(true)
                .withKind("SuperSecret")
                .withApiVersion("operators.on.hands/v1alpha1")
                .withName(superSecret.getMetadata().getName())
                .withNewUid(superSecret.getMetadata().getUid())
                .endOwnerReference()
                .endMetadata()
                .build();

    }
}
