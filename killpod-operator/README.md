# Kill Pod Operator

This is a super simple Chaos monkey style operator inspired by Kubemonkey. It
will regularly kill the pods of deployments that are `killpod/enabled`.

## Building

```
$ ./gradlew clean ass
$ ./gradlew reflectionConfigGenerator

# this will build the native image for the local platform
$ ./gradlew graalNativeImage

# this will build the native image for Linux to run in K8s
$ docker build -t killpod-operator:1.0 .
```

## Using

To deploy the operator, apply the following K8s descriptor files:
```
$ cd killpod-operator
$ kubectl apply -f src/main/resources/killpod-rbac.yaml
$ kubectl apply -f src/main/resources/killpod-operator.yaml
```

To enable a deployment for the killpod-operator, you need to add a few labels
to the metadata of the deployment. The `killpod/application` label also needs
to be added to the pod template metadata, and the values need to match.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-killpod-enabled
  labels:
    killpod/enabled: "true"
    killpod/application: nginx-killpod-enabled
    killpod/delay: "30"
    killpod/amount: "2"
spec:
  selector:
    matchLabels:
      app: nginx-killpod-enabled
  replicas: 4
  template:
    metadata:
      labels:
        app: nginx-killpod-enabled
        killpod/application: nginx-killpod-enabled
    spec:
      containers:
        - name: nginx
          image: nginx:1.17.6-alpine
          ports:
            - containerPort: 80
```
