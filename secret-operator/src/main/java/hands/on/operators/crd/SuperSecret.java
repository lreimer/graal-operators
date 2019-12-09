package hands.on.operators.crd;

import io.fabric8.kubernetes.client.CustomResource;

import java.util.StringJoiner;

public class SuperSecret extends CustomResource {

    private SuperSecretSpec spec;
    private SuperSecretStatus status;

    public SuperSecretSpec getSpec() {
        return spec;
    }

    public void setSpec(SuperSecretSpec spec) {
        this.spec = spec;
    }

    public SuperSecretStatus getStatus() {
        return status;
    }

    public void setStatus(SuperSecretStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SuperSecret.class.getSimpleName() + "[", "]")
                .add("apiVersion=" + getApiVersion())
                .add("metadata=" + getMetadata())
                .add("spec=" + spec)
                .add("status=" + status)
                .toString();
    }
}
