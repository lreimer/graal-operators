# Kubernetes Operators with GraalVM

The repository contains different K8s operator implementations in Java using GraalVM.

## GraalVM installation

I used Jabba as a JVM manager to install GraalVM on my machine. Once this is done,
make sure to install the `native-image` using the Graal updater.

```
# use Jabba to install GraalVM
jabba ls-remote
jabba install graalvm@19.3.0
jabba use graalvm@19.3.0

export GRAALVM_HOME=$JAVA_HOME

gu available
gu install llvm-toolchain
gu install native-image
```

## KillPod Operator

This is a super simple Chaos monkey style operator inspired by Kubemonkey. It
will regularly kill the pods of deployments that are `killpod/enabled`. The
usage instructions are found [here](killpod-operator/README.md).

## Microservice Operator

## Super Secret Operator

## Maintainer

M.-Leander Reimer (@lreimer), <mario-leander.reimer@qaware.de>

## License

This software is provided under the MIT open source license, read the `LICENSE`
file for details.
