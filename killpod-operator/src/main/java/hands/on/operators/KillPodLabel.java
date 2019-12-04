package hands.on.operators;

/**
 * Enum for all the labels relevant to the KillPod operator.
 */
public enum KillPodLabel {
    KILLPOD_ENABLED("killpod/enabled"),
    KILLPOD_APPLICATION("killpod/application"),
    KILLPOD_DELAY("killpod/delay"),
    KILLPOD_AMOUNT("killpod/amount");

    private final String key;

    KillPodLabel(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
