package hands.on.operators;

import hands.on.operators.crd.DoneableSuperSecret;
import hands.on.operators.crd.SuperSecret;
import hands.on.operators.crd.SuperSecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

/**
 * Controller and reconciliation loop for this operator.
 */
public class SuperSecretController {

    private final KubernetesClient client;
    private final MixedOperation<SuperSecret, SuperSecretList, DoneableSuperSecret, Resource<SuperSecret, DoneableSuperSecret>> superSecretClient;
    private final SharedIndexInformer<SuperSecret> superSecretInformer;
    private final String namespace;

    public SuperSecretController(KubernetesClient client, MixedOperation<SuperSecret, SuperSecretList, DoneableSuperSecret, Resource<SuperSecret, DoneableSuperSecret>> superSecretClient, SharedIndexInformer<SuperSecret> superSecretInformer, String namespace) {
        this.client = client;
        this.superSecretClient = superSecretClient;
        this.superSecretInformer = superSecretInformer;
        this.namespace = namespace;
    }

    /**
     * Register for SuperSecret change events.
     */
    public void create() {
        superSecretInformer.addEventHandler(new ResourceEventHandler<SuperSecret>() {
            @Override
            public void onAdd(SuperSecret obj) {

            }

            @Override
            public void onUpdate(SuperSecret oldObj, SuperSecret newObj) {

            }

            @Override
            public void onDelete(SuperSecret obj, boolean deletedFinalStateUnknown) {

            }
        });
    }

    public void run() {

    }
}
