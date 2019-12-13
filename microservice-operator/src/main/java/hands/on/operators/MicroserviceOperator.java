package hands.on.operators;

import hands.on.operators.crd.Microservice;
import hands.on.operators.crd.MicroserviceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main application class for the MicroserviceOperator operator. Bootstrapping of CLI and main loop.
 */
@Command(version = "Super Secret Operator 1.0", mixinStandardHelpOptions = true)
class MicroserviceOperator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceOperator.class);

    @CommandLine.Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "the K8s namespace")
    private String namespace;

    public static void main(String[] args) {
        CommandLine.run(new MicroserviceOperator(), args);
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
                    .withPlural("microservices")
                    .build();

            SharedInformerFactory informerFactory = client.informers();
            SharedIndexInformer<Microservice> superSecretInformer = informerFactory.sharedIndexInformerForCustomResource(superSecretCustomResourceDefinitionContext, Microservice.class, MicroserviceList.class, 5 * 1000);

            MicroserviceController microserviceController = new MicroserviceController(client, superSecretInformer, namespace);
            microserviceController.create();

            informerFactory.startAllRegisteredInformers();

            microserviceController.run();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error running operator.", e);
        }
    }
}
