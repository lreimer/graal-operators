package hands.on.operators.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableMicroservice extends CustomResourceDoneable<Microservice> {
    public DoneableMicroservice(Microservice resource, Function<Microservice, Microservice> function) {
        super(resource, function);
    }
}
