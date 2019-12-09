package hands.on.operators.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.StringJoiner;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class SuperSecretSpec implements KubernetesResource {

    private String encryptedSecretValue;

    public String getEncryptedSecretValue() {
        return encryptedSecretValue;
    }

    public void setEncryptedSecretValue(String encryptedSecretValue) {
        this.encryptedSecretValue = encryptedSecretValue;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SuperSecretSpec.class.getSimpleName() + "[", "]")
                .add("encryptedSecretValue='" + encryptedSecretValue + "'")
                .toString();
    }
}
