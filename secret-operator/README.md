# Super Secret Operator

This operator allows you to apply super encrypted secrets, the operator will
then decrypt and manage the ordinary secrets in Kubernetes. Inspired by Sealed Secrets
from Bitnami found here https://github.com/bitnami-labs/sealed-secrets.

## Building

```
$ ./gradlew clean ass
$ ./gradlew run --args="-n default -p src/test/resources/publicKey"

# this will build the native image for the local platform
$ ./gradlew reflectionConfigGenerator
$ ./gradlew graalNativeImage

# this will build the native image for Linux to run in K8s
$ docker build -t secret-operator:1.0 .
```

## Using

To deploy the operator , apply the following K8s descriptor files:

```
$ kubectl apply -f src/main/resources/supersecret-rbac.yaml
$ kubectl apply -f src/main/resources/supersecret-crd.yaml

$ kubectl apply -f src/test/resources/supersecret-secret.yaml
$ kubectl apply -f src/main/resources/supersecret-operator.yaml
```

:exclamation: The used secret is for testing only! Make sure to use a different RSA key for production!

First, generate a new key pair using the supplied utility program and create a generic secret for the public key.
```
$ ./gradlew generateKeyPair
$ kubectl create secret generic supersecret-secret --from-file=publicKey=src/test/resources/publicKey
$ kubectl get secret supersecret-secret -o yaml

$ ./gradlew encryptSuperSecretPassword
```

With the encrypted password you can now create a `SuperSecret` resource with the encrypted value, e.g.

```yaml
apiVersion: operators.on.hands/v1alpha1
kind: SuperSecret
metadata:
  name: supersecret-test
spec:
  secretData:
    password: eV7YoQXyZlY+y51RWXEqyu0U44EPEPwEz+fZvGo+7McOTA4wQYCdxXMJ8D1aiHDNorYBmMvOWB/hsUlBvSJDwOEufkpX9AAbpvrMf4U5LrMcC/yhyi5ERG0zarimXVhc0R8TORlCSN0YH5AlcvVl2p/A2omL9/ANtab3aW8ywqpkHYtSLvrPgFnbcuSvD2UzuUNeE2qkh6SAABKC4A0ox3Lc02oVjpApe57xL+sfAm+I47c/3ip7kfH+xdeGhTJrWIqBaYi2gsfIEsSXQRpwuVUuL57wp8pNgvF2NpwYd6ZLM5b0zPnHUwM1z9Lpfwi+IUZjaY34Z+RjEL5OZFPYkQ==
```

When you apply the `SuperSecret`, the operator should pick up and create an ordinary `Secret` with the decrypted value.

```bash
$ kubectl apply -f src/test/resources/supersecret-test.yaml
$ kubectl get secret supersecret-test -o yaml
$ echo 'U3VwZXJTZWNyZXRQYXNzd29yZA==' | base64 --decode
```
