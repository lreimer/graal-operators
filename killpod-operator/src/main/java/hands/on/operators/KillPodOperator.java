package hands.on.operators;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application for the KillPod operator.
 */
@Command(version = "Kill Pod Operator 1.0", mixinStandardHelpOptions = true)
class KillPodOperator implements Runnable {

    private static final Logger logger = Logger.getLogger(KillPodOperator.class.getName());

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
                logger.log(Level.INFO, "Using namespace from K8s client config.");
                namespace = client.getNamespace();
            }

            SharedInformerFactory informerFactory = client.informers();

            SharedIndexInformer<Deployment> deploymentInformer = informerFactory.sharedIndexInformerFor(Deployment.class, DeploymentList.class, 10 * 1000);

            KillPodController killPodController = new KillPodController(client, deploymentInformer, namespace, timeUnit);
            killPodController.create();

            logger.log(Level.INFO, "Starting all registered informers.");
            informerFactory.startAllRegisteredInformers();

            logger.log(Level.INFO, "Running operator ...");
            killPodController.run();
        }
    }
}
