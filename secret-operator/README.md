# Super Secret Operator

This operator allows you to apply super encrypted secrets, the operator will
then decrypt and manage the ordinary secrets in Kubernetes. Inspired by Sealed Secrets
from Bitnami found here https://github.com/bitnami-labs/sealed-secrets.

## Building

```
$ ./gradlew clean ass
$ ./gradlew reflectionConfigGenerator

# this will build the native image for the local platform
$ ./gradlew graalNativeImage

# this will build the native image for Linux to run in K8s
$ docker build -t secret-operator:1.0 .
```

## Using
