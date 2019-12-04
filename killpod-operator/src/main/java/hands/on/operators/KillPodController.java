package hands.on.operators;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static hands.on.operators.KillPodLabel.*;

/**
 * The controller handles the reconciliation loop of the KillPod controller.
 */
public class KillPodController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KillPodController.class);

    private final KubernetesClient client;
    private final String namespace;
    private final TimeUnit timeUnit;
    private final BlockingQueue<Deployment> workqueue;
    private final ScheduledExecutorService executor;
    private final ConcurrentMap<String, ScheduledFuture<?>> killJobs;
    private final ConcurrentMap<String, String> revisions;

    public KillPodController(KubernetesClient client, String namespace, TimeUnit timeUnit) {
        this.client = client;
        this.namespace = namespace;
        this.timeUnit = timeUnit;
        this.workqueue = new ArrayBlockingQueue<>(256);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.killJobs = new ConcurrentHashMap<>(8);
        this.revisions = new ConcurrentHashMap<>(8);
    }

    public void create() {
        LOGGER.info("Initializing KillPod controller and deployment watcher.");
        client.apps().deployments().inNamespace(namespace).watch(new Watcher<Deployment>() {
            @Override
            public void eventReceived(Action action, Deployment deployment) {
                if (Action.ADDED.equals(action) || Action.MODIFIED.equals(action)) {
                    enqueueDeployment(deployment);
                } else if (Action.DELETED.equals(action)) {
                    cancelKillJob(deployment);
                } else {
                    String name = deployment.getMetadata().getName();
                    LOGGER.warn("Error watching deployment {}.", name);
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
            }
        });
    }

    public void run() {
        LOGGER.info("Running KillPod controller ...");

        while (true) {
            try {
                LOGGER.info("Trying to fetch deployment from work queue...");
                if (workqueue.isEmpty()) {
                    LOGGER.info("Work queue is empty.");
                }

                Deployment deployment = workqueue.take();
                LOGGER.info("Got deployment {} from work queue.", deployment.getMetadata().getName());
                reconcile(deployment);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("KillPod controller interrupted.");
            }
        }
    }

    private void reconcile(Deployment deployment) {
        LOGGER.info("Reconcile deployment {}.", deployment.getMetadata().getName());

        String name = deployment.getMetadata().getName();
        Map<String, String> labels = Optional.ofNullable(deployment.getMetadata().getLabels()).orElse(Collections.emptyMap());
        boolean enabled = Boolean.parseBoolean(labels.getOrDefault(KILLPOD_ENABLED.getKey(), Boolean.FALSE.toString()));

        if (!enabled) {
            // maybe switch from enabled to disabled
            LOGGER.info("Deployment {} not enabled. Skipping ...", name);
            cancelKillJob(deployment);
            return;
        }

        if (hasKillJob(deployment)) {
            // then we have an updated deployment, cancel running job
            LOGGER.info("Cancel existing kill jobs for Deployment {}", name);
            cancelKillJob(deployment);
        }

        scheduleKillJob(deployment);
    }

    private void enqueueDeployment(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        Map<String, String> annotations = deployment.getMetadata().getAnnotations();
        String revision = annotations.getOrDefault("deployment.kubernetes.io/revision", "1");

        // check if deployment has changed
        if (!revision.equals(revisions.getOrDefault(name, "0"))) {
            LOGGER.info("Going to enqueue deployment {} with revision {} to reconcile.", name, revision);
            revisions.put(name, revision);
            workqueue.add(deployment);
        } else {
            LOGGER.info("No change detected for deployment {} with revision {}.", name, revision);
        }

    }

    private void scheduleKillJob(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        LOGGER.info("Schedule kill job for application {}.", name);

        Map<String, String> labels = Optional.ofNullable(deployment.getMetadata().getLabels()).orElse(Collections.emptyMap());
        String application = labels.getOrDefault(KILLPOD_APPLICATION.getKey(), name);
        long delay = Long.parseLong(labels.getOrDefault(KILLPOD_DELAY.getKey(), "60"));
        int amount = Integer.parseInt(labels.getOrDefault(KILLPOD_AMOUNT.getKey(), "1"));

        Runnable command = new KillPodExecutor(client, namespace, application, amount);
        ScheduledFuture<?> killJob = executor.scheduleWithFixedDelay(command, delay, delay, timeUnit);
        killJobs.put(name, killJob);
    }

    private boolean hasKillJob(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        return killJobs.containsKey(name);
    }

    private void cancelKillJob(Deployment deployment) {
        LOGGER.info("Cancel kill job for application {}.", deployment.getMetadata().getName());
        String name = deployment.getMetadata().getName();

        ScheduledFuture<?> future = killJobs.remove(name);
        if (future != null) {
            future.cancel(true);
        }

        revisions.remove(name);
    }
}
