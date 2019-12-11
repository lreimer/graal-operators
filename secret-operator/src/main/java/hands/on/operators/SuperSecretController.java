package hands.on.operators;

import hands.on.operators.crd.SuperSecret;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
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
    private final SharedIndexInformer<SuperSecret> superSecretInformer;
    private final BlockingQueue<SuperSecret> workqueue;
    private final String namespace;
    private final File publicKeyFile;
    private final Cipher cipher;

    public SuperSecretController(KubernetesClient client, SharedIndexInformer<SuperSecret> superSecretInformer, String namespace, File publicKeyFile) throws GeneralSecurityException {
        this.client = client;
        this.superSecretInformer = superSecretInformer;
        this.workqueue = new ArrayBlockingQueue<>(8);
        this.namespace = namespace;
        this.publicKeyFile = publicKeyFile;
        this.cipher = Cipher.getInstance("RSA");
    }

    /**
     * Register for SuperSecret change events.
     */
    public void create() throws GeneralSecurityException, IOException {
        LOGGER.info("Creating SuperSecretController ...");

        byte[] keyBytes = Files.readAllBytes(publicKeyFile.toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        this.cipher.init(Cipher.DECRYPT_MODE, publicKey);

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
        Map<String, String> secretData = secret.getData();

        superSecret.getSpec().getSecretData().forEach((k, v) -> secretData.put(k, decrypt(v)));

        client.secrets().inNamespace(namespace).createOrReplace(secret);
    }

    private String decrypt(String superSecretValue) {
        byte[] decode = Base64.getDecoder().decode(superSecretValue);
        try {
            return new String(cipher.doFinal(decode), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Unable to decrypt super secret value.", e);
            return superSecretValue;
        }
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
