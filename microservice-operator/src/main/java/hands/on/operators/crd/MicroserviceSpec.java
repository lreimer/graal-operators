package hands.on.operators.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class MicroserviceSpec implements KubernetesResource {

    private Integer replicas = 1;
    private String image;
    private List<ContainerPort> ports = new ArrayList<>();
    private String serviceType = "Cluster";

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<ContainerPort> getPorts() {
        return ports;
    }

    public void setPorts(List<ContainerPort> ports) {
        this.ports = ports;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MicroserviceSpec.class.getSimpleName() + "[", "]")
                .add("replicas=" + replicas)
                .add("image='" + image + "'")
                .add("ports=" + ports)
                .add("serviceType='" + serviceType + "'")
                .toString();
    }
}
