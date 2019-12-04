package hands.on.operators;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.TimeUnit;

/**
 * Main application for the KillPod operator.
 */
@Command(version = "Kill Pod Operator 1.0", mixinStandardHelpOptions = true)
class KillPodOperator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KillPodOperator.class);

    @Option(names = {"-n", "--namespace"}, defaultValue = "default", description = "the K8s namespace")
    private String namespace;

    @Option(names = {"-t", "--time"}, defaultValue = "SECONDS", description = "the time unit")
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    public static void main(String[] args) {
        CommandLine.run(new KillPodOperator(), args);
    }

    @Override
    public void run() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            if (client.getNamespace() != null) {
                LOGGER.debug("Using namespace from K8s client config.");
                namespace = client.getNamespace();
            }

            KillPodController killPodController = new KillPodController(client, namespace, timeUnit);
            killPodController.create();
            killPodController.run();
        }
    }
}
