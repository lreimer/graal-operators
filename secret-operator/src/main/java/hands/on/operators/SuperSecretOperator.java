package hands.on.operators;

import hands.on.operators.crd.SuperSecret;
import hands.on.operators.crd.SuperSecretList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Main application class for the Super Secret operator. Bootstrapping of CLI and main loop.
 */
@Command(version = "Super Secret Operator 1.0", mixinStandardHelpOptions = true)
class SuperSecretOperator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperSecretOperator.class);

    @CommandLine.Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "the K8s namespace")
    private String namespace;

    @CommandLine.Option(names = {"-p", "--publicKey"}, defaultValue = "/publicKey", description = "the path to the public key file")
    private String publicKey;

    public static void main(String[] args) {
        CommandLine.run(new SuperSecretOperator(), args);
    }

    @Override
    public void run() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            if (client.getNamespace() != null) {
                LOGGER.debug("Using namespace from K8s client config.");
                namespace = client.getNamespace();
            }

            CustomResourceDefinitionContext superSecretCustomResourceDefinitionContext = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1alpha1")
                    .withScope("Namespaced")
                    .withGroup("operators.on.hands")
                    .withPlural("supersecrets")
                    .build();

            SharedInformerFactory informerFactory = client.informers();
            SharedIndexInformer<SuperSecret> superSecretInformer = informerFactory.sharedIndexInformerForCustomResource(superSecretCustomResourceDefinitionContext, SuperSecret.class, SuperSecretList.class, 5 * 1000);

            SuperSecretController superSecretController = new SuperSecretController(client, superSecretInformer, namespace, new File(publicKey));
            superSecretController.create();

            informerFactory.startAllRegisteredInformers();

            superSecretController.run();
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error initializing Crypto.", e);
        } catch (IOException e) {
            LOGGER.error("Error reading public key.", e);
        }
    }
}
