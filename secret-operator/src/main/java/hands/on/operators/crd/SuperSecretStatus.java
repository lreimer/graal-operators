package hands.on.operators.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.StringJoiner;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class SuperSecretStatus implements KubernetesResource {
}
