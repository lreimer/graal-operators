FROM oracle/graalvm-ce:19.2.1 AS builder

RUN gu install native-image && mkdir /killpod-operator

COPY build/reflect.json /killpod-operator/
COPY build/libs/ /killpod-operator/libs

WORKDIR /killpod-operator

RUN native-image \
    -cp libs/jackson-dataformat-yaml-2.9.9.jar:libs/jackson-core-2.9.9.jar:libs/jackson-databind-2.9.9.jar:libs/picocli-codegen-3.9.6.jar:libs/slf4j-simple-1.7.26.jar:libs/javax.el-3.0.1-b11.jar:libs/okhttp-3.12.0.jar:libs/slf4j-api-1.7.26.jar:libs/zjsonpatch-0.3.0.jar:libs/logging-interceptor-3.12.0.jar:libs/kubernetes-model-common-4.3.1.jar:libs/snakeyaml-1.23.jar:libs/kubernetes-client-4.3.1.jar:libs/jackson-module-jaxb-annotations-2.9.9.jar:libs/jackson-annotations-2.9.0.jar:libs/picocli-3.9.6.jar:libs/validation-api-2.0.1.Final.jar:libs/jul-to-slf4j-1.7.26.jar:libs/okio-1.15.0.jar:libs/killpod-operator.jar:libs/generex-1.0.2.jar:libs/jansi-1.18.jar:libs/kubernetes-model-4.3.1.jar:libs/automaton-1.11-8.jar \
    -H:ReflectionConfigurationFiles=reflect.json \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:+ReportExceptionStackTraces \
    -H:+AddAllCharsets \
    --initialize-at-run-time=org.fusesource.jansi.WindowsAnsiOutputStream \
    --no-server \
    --enable-http \
    --enable-https \
    hands.on.operators.KillPodOperator \
    killpodop

# maybe use a layered docker build here instead

ENTRYPOINT ["/killpod-operator/killpodop"]
CMD ["-n", "default", "-t", "SECONDS"]
