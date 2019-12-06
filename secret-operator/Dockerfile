FROM oracle/graalvm-ce:19.3.0 AS builder

RUN gu install native-image && mkdir /secret-operator

COPY build/extended-reflect.json /secret-operator/
COPY build/libs/ /secret-operator/libs

WORKDIR /secret-operator

RUN native-image \
    -cp libs/okhttp-3.12.6.jar:libs/jakarta.xml.bind-api-2.3.2.jar:libs/picocli-codegen-3.9.6.jar:libs/logging-interceptor-3.12.6.jar:libs/slf4j-simple-1.7.26.jar:libs/kubernetes-client-4.6.3.jar:libs/zjsonpatch-0.3.0.jar:libs/jackson-core-2.10.0.jar:libs/bcprov-ext-jdk15on-1.62.jar:libs/kubernetes-model-common-4.6.3.jar:libs/snakeyaml-1.24.jar:libs/picocli-3.9.6.jar:libs/jackson-dataformat-yaml-2.10.0.jar:libs/jakarta.activation-api-1.2.1.jar:libs/kubernetes-model-4.6.3.jar:libs/jackson-module-jaxb-annotations-2.10.0.jar:libs/okio-1.15.0.jar:libs/slf4j-api-1.7.29.jar:libs/bcprov-jdk15on-1.62.jar:libs/secret-operator.jar:libs/generex-1.0.2.jar:libs/jansi-1.18.jar:libs/bcpkix-jdk15on-1.62.jar:libs/jackson-annotations-2.10.0.jar:libs/jackson-databind-2.10.0.jar:libs/automaton-1.11-8.jar \
    -H:ReflectionConfigurationFiles=extended-reflect.json \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:+ReportExceptionStackTraces \
    -H:+AddAllCharsets \
    --initialize-at-run-time=org.fusesource.jansi.WindowsAnsiOutputStream \
    --no-server \
    --enable-http \
    --enable-https \
    hands.on.operators.SuperSecretOperator \
    secretop

FROM gcr.io/distroless/cc-debian10:debug
MAINTAINER Mario-Leander Reimer <mario-leander.reimer@qaware.de>
LABEL version="1.0"

COPY --from=builder /secret-operator/secretop /
COPY --from=builder /opt/graalvm-ce-19.3.0/jre/lib/amd64/libsunec.so /

COPY --from=debian:10.2 /usr/lib/x86_64-linux-gnu/libz* /usr/lib/x86_64-linux-gnu/
COPY --from=debian:10.2 /lib/x86_64-linux-gnu/libz* /lib/x86_64-linux-gnu/

ENTRYPOINT ["/secretop"]
CMD ["-n", "default", "-t", "SECONDS"]
