# Microservice Operator

This operator aims to make developer live a lot easier by abstracting the usual
`Deployment`, `Service` and `ConfigMap` definitions using a simple and unified
`Microservice` custom resource. The operator will then manage the underlying
Kubernetes resources automagically.

## Building

```
$ ./gradlew clean ass
$ ./gradlew reflectionConfigGenerator

# this will build the native image for the local platform
$ ./gradlew graalNativeImage

# this will build the native image for Linux to run in K8s
$ docker build -t microservice-operator:1.0 .
```

## Using
