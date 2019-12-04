package hands.on.operators;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static hands.on.operators.KillPodLabel.KILLPOD_APPLICATION;

/**
 * This class actually does the killing of the pods.
 */
public class KillPodExecutor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KillPodExecutor.class);

    private final KubernetesClient client;
    private final String namespace;
    private final String application;
    private final int amount;

    public KillPodExecutor(KubernetesClient client, String namespace, String application, int amount) {
        this.client = client;
        this.namespace = namespace;
        this.application = application;
        this.amount = amount;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Kill pods for application {}.", application);
            PodList podList = client.pods().inNamespace(namespace).withLabel(KILLPOD_APPLICATION.getKey(), application).list();
            List<Pod> pods = podList.getItems();

            int toIndex;
            if (amount < 0 || amount >= pods.size()) {
                toIndex = pods.size() - 1;
            } else {
                toIndex = amount;
            }

            LOGGER.info("Deleting {} pods for application {}.", toIndex, application);
            client.pods().inNamespace(namespace).delete(pods.subList(0, toIndex));
        } catch (KubernetesClientException e) {
            LOGGER.warn("Unable to kill pods for application " + application, e);
        }
    }
}
