package hands.on.operators;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Optional;

@Command(version = "Super Secret Operator 1.0", mixinStandardHelpOptions = true)
class SuperSecretOperator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperSecretOperator.class);

    @CommandLine.Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "the K8s namespace")
    private String namespace;

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

            SuperSecretController superSecretController = new SuperSecretController(client, namespace);
            superSecretController.create();
            superSecretController.run();
        }
    }
}
