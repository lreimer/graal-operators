# Microservice Operator

This operator aims to make developer live a lot easier by abstracting the usual
`Deployment`, `Service` and `ConfigMap` definitions using a simple and unified
`Microservice` custom resource. The operator will then manage the underlying
Kubernetes resources automagically.

:exclamation: Currently the operator only creates a basic deployment and service object.

This operator is just a demo. We should create liveness and readiness probes, and also resource constraints for
the create deployment. Also there currently is no ConfigMap support.

## Building

```
$ ./gradlew kick ass
$ ./gradlew run

# this will build the native image for the local platform
$ export GRAALVM_HOME=$JAVA_HOME
$ ./gradlew reflectionConfigGenerator
$ ./gradlew graalNativeImage

$ cd build
$ ./microserviceop -n default

# this will build the native image for Linux to run in K8s
$ docker build -t microservice-operator:1.0 .
```

## Using

To deploy the operator , apply the following K8s descriptor files:

```
$ kubectl apply -f src/main/resources/microservice-rbac.yaml
$ kubectl apply -f src/main/resources/microservice-crd.yaml

$ kubectl apply -f src/main/resources/microservice-operator.yaml

$ kubectl apply -f src/test/resources/microservice-test.yaml
```
