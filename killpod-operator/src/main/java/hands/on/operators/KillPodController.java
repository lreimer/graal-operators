package hands.on.operators;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Lister;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KillPodController {

    private static final Logger logger = Logger.getLogger(KillPodController.class.getName());

    private static final String KILLPOD_ENABLED_KEY = "killpod/enabled";
    private static final String KILLPOD_APPLICATION_KEY = "killpod/application";
    private static final String KILLPOD_DELAY_KEY = "killpod/delay";
    private static final String KILLPOD_AMOUNT_KEY = "killpod/amount";

    private final KubernetesClient client;
    private final SharedIndexInformer<Deployment> deploymentInformer;
    private final String namespace;
    private final TimeUnit timeUnit;
    private final BlockingQueue<String> workqueue;
    private final ScheduledExecutorService executor;
    private final ConcurrentMap<String, ScheduledFuture<?>> killJobs;
    private final Lister<Deployment> deploymentLister;

    public KillPodController(KubernetesClient client, SharedIndexInformer<Deployment> deploymentInformer, String namespace, TimeUnit timeUnit) {
        this.client = client;
        this.deploymentInformer = deploymentInformer;
        this.deploymentLister = new Lister<>(deploymentInformer.getIndexer(), namespace);
        this.namespace = namespace;
        this.timeUnit = timeUnit;
        this.workqueue = new ArrayBlockingQueue<>(256);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.killJobs = new ConcurrentHashMap<>(8);
    }

    public void create() {
        logger.log(Level.INFO, "Creating KillPod controller and deployment informer.");
        deploymentInformer.addEventHandler(new ResourceEventHandler<Deployment>() {
            @Override
            public void onAdd(Deployment deployment) {
                enqueueDeployment(deployment);
            }

            @Override
            public void onUpdate(Deployment oldDeployment, Deployment newDeployment) {
                if (Objects.equals(oldDeployment.getMetadata().getResourceVersion(), newDeployment.getMetadata().getResourceVersion())) {
                    return;
                }
                enqueueDeployment(newDeployment);
            }

            @Override
            public void onDelete(Deployment deployment, boolean b) {
                cancelKillJob(deployment);
            }
        });
    }

    public void run() {
        logger.log(Level.INFO, "Starting KillPod controller.");
        while (!deploymentInformer.hasSynced()) ;

        while (true) {
            try {
                logger.log(Level.INFO, "Trying to fetch deployment from work queue...");
                if (workqueue.isEmpty()) {
                    logger.log(Level.INFO, "Work queue is empty.");
                }

                String name = workqueue.take();
                logger.log(Level.INFO, "Got deployment {0} from work queue.", name);

                Deployment deployment = deploymentLister.get(name);
                if (deployment == null) {
                    logger.log(Level.WARNING, "Deployment {0} in work queue no longer exists. Continue.");
                } else {
                    reconcile(deployment);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.WARNING, "KillPod controller interrupted.");
            }
        }
    }

    private void reconcile(Deployment deployment) {
        Map<String, String> annotations = deployment.getMetadata().getAnnotations();
        boolean enabled = Boolean.parseBoolean(annotations.getOrDefault(KILLPOD_ENABLED_KEY, Boolean.FALSE.toString()));

        if (!enabled) {
            // maybe switch from enabled to disabled
            cancelKillJob(deployment);
            return;
        }

        if (hasKillJob(deployment)) {
            // then we have an updated deployment, cancel running job
            cancelKillJob(deployment);
        }

        scheduleKillJob(deployment);
    }

    private void enqueueDeployment(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        logger.log(Level.INFO, "Going to enqueue deployment {0} to reconcile.", name);
        workqueue.add(name);
    }

    private void scheduleKillJob(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        Map<String, String> annotations = deployment.getMetadata().getAnnotations();
        String application = annotations.getOrDefault(KILLPOD_APPLICATION_KEY, name);
        long delay = Long.parseLong(annotations.getOrDefault(KILLPOD_DELAY_KEY, "60"));
        int amount = Integer.parseInt(annotations.getOrDefault(KILLPOD_AMOUNT_KEY, "1"));

        Runnable command = () -> killPods(application, amount);
        ScheduledFuture<?> killJob = executor.scheduleWithFixedDelay(command, delay, delay, timeUnit);
        killJobs.put(name, killJob);
    }

    private void killPods(String application, int amount) {
        PodList podList = client.pods().inNamespace(namespace).withLabel(KILLPOD_APPLICATION_KEY, application).list();
        List<Pod> pods = podList.getItems();

        int toIndex;
        if (amount < 0 || amount >= pods.size()) {
            toIndex = pods.size() - 1;
        } else {
            toIndex = amount;
        }

        client.pods().inNamespace(namespace).delete(pods.subList(0, toIndex));
    }

    private boolean hasKillJob(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        return killJobs.containsKey(name);
    }

    private void cancelKillJob(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        ScheduledFuture<?> future = killJobs.remove(name);
        if (future != null) {
            future.cancel(true);
        }
    }
}
