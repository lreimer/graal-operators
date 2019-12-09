package hands.on.operators.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableSuperSecret extends CustomResourceDoneable<SuperSecret> {
    public DoneableSuperSecret(SuperSecret resource, Function<SuperSecret, SuperSecret> function) {
        super(resource, function);
    }
}
