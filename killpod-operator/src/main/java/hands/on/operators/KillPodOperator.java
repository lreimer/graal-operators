package hands.on.operators;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Optional;

@Command(version = "Kill Pod Operator 1.0", mixinStandardHelpOptions = true)
class KillPodOperator implements Runnable {

    @Parameters(arity = "0..1", description = "The message to echo.")
    private String message;

    public static void main(String[] args) {
        CommandLine.run(new KillPodOperator(), args);
    }

    @Override
    public void run() {
        String output = Optional.ofNullable(message).orElse("Kill Pod Operator 1.0");
        System.out.println(output);
    }
}
