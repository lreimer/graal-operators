package hands.on.operators.crd;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.StringJoiner;

public class Microservice extends CustomResource {

    private MicroserviceSpec spec;
    private MicroserviceStatus status;

    public MicroserviceSpec getSpec() {
        return spec;
    }

    public void setSpec(MicroserviceSpec spec) {
        this.spec = spec;
    }

    public MicroserviceStatus getStatus() {
        return status;
    }

    public void setStatus(MicroserviceStatus status) {
        this.status = status;
    }

    @Override
    public ObjectMeta getMetadata() {
        return super.getMetadata();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Microservice.class.getSimpleName() + "[", "]")
                .add("apiVersion=" + getApiVersion())
                .add("metadata=" + getMetadata())
                .add("spec=" + spec)
                .add("status=" + status)
                .toString();
    }
}
