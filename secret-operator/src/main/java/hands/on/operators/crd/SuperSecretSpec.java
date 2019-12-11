package hands.on.operators.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class SuperSecretSpec implements KubernetesResource {

    private Map<String, String> secretData = new HashMap<>();

    public Map<String, String> getSecretData() {
        return secretData;
    }

    public void setSecretData(Map<String, String> secretData) {
        this.secretData = secretData;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SuperSecretSpec.class.getSimpleName() + "[", "]")
                .add("secretData=" + secretData)
                .toString();
    }
}
