# Listing 6: Java-Template fuer Docker Sandboxes (sbx), Downloads mit Checksumme.
# Basis -docker statt -minimal: nur sie hat einen Docker-Daemon (Testcontainers).
FROM docker/sandbox-templates:claude-code-docker

# TARGETARCH wird von buildx gesetzt: amd64 | arm64
ARG TARGETARCH

ARG JDK_VERSION=25.0.4.1+1
ARG MAVEN_VERSION=3.10.0
ARG GRADLE_VERSION=9.8.1
ARG JBANG_VERSION=0.142.0
ARG JBANG_SHA256=8bee0bd035344cc46322aff7117ec475d48cefe37f2a79ff2c6ee3db577bb070

# Offizielle Checksummen der Releases
ARG JDK_SHA256_amd64=dbb698396d478e7fa2b1e50f4103324b2a99b90569ee27c33f2261f9215cf41e
ARG JDK_SHA256_arm64=69df11a02cfa3ef7d7ca645e03edce6778ec090e100f6ae2b42097865730ac52
ARG MAVEN_SHA512=908b1501bfb420bf7c8affb855534a9c407fd6099367bfb9f2f2dcb8e9799102bffb84518cde74c679bd76870247c6528683abdd620581bffa90f95d92d175aa
ARG GRADLE_SHA256=dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f

USER root
SHELL ["/bin/bash", "-euo", "pipefail", "-c"]

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates tar unzip \
 && rm -rf /var/lib/apt/lists/*

# --- Temurin 25 JDK ---
RUN set -euo pipefail; \
    case "$TARGETARCH" in \
      amd64) jarch=x64;     sha="$JDK_SHA256_amd64" ;; \
      arm64) jarch=aarch64; sha="$JDK_SHA256_arm64" ;; \
      *) echo "nicht unterstuetzte Architektur: $TARGETARCH" >&2; exit 1 ;; \
    esac; \
    ver="${JDK_VERSION/+/_}"; tag="jdk-${JDK_VERSION/+/%2B}"; \
    url="https://github.com/adoptium/temurin25-binaries/releases/download/${tag}/OpenJDK25U-jdk_${jarch}_linux_hotspot_${ver}.tar.gz"; \
    curl -fsSL "$url" -o /tmp/jdk.tgz; \
    echo "${sha}  /tmp/jdk.tgz" | sha256sum -c -; \
    mkdir -p /opt/java; tar -xzf /tmp/jdk.tgz -C /opt/java --strip-components=1; \
    rm /tmp/jdk.tgz

# --- Maven 3.10.0 ---
RUN set -euo pipefail; \
    url="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"; \
    curl -fsSL "$url" -o /tmp/mvn.tgz; \
    echo "${MAVEN_SHA512}  /tmp/mvn.tgz" | sha512sum -c -; \
    tar -xzf /tmp/mvn.tgz -C /opt; ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven; \
    rm /tmp/mvn.tgz

# --- Gradle 9.8.1 ---
RUN set -euo pipefail; \
    url="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"; \
    curl -fsSL "$url" -o /tmp/gradle.zip; \
    echo "${GRADLE_SHA256}  /tmp/gradle.zip" | sha256sum -c -; \
    unzip -q /tmp/gradle.zip -d /opt; ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle; \
    rm /tmp/gradle.zip

# --- JBang 0.142.0 (plattformunabhaengig, fuer den PreToolUse-Hook) ---
RUN set -euo pipefail; \
    url="https://github.com/jbangdev/jbang/releases/download/v${JBANG_VERSION}/jbang-${JBANG_VERSION}.zip"; \
    curl -fsSL "$url" -o /tmp/jbang.zip; \
    echo "${JBANG_SHA256}  /tmp/jbang.zip" | sha256sum -c -; \
    unzip -q /tmp/jbang.zip -d /opt; ln -s /opt/jbang-${JBANG_VERSION} /opt/jbang; \
    rm /tmp/jbang.zip

ENV JAVA_HOME=/opt/java \
    MAVEN_HOME=/opt/maven \
    GRADLE_HOME=/opt/gradle \
    JBANG_DIR=/home/agent/.jbang \
    PATH=/opt/java/bin:/opt/maven/bin:/opt/gradle/bin:/opt/jbang/bin:$PATH

# Managed Settings: Deny-Regeln, Mindestversion, OS-Sandbox aus (stoert Testcontainers).
COPY managed-settings.json /etc/claude-code/managed-settings.json

# Zurueck auf den unprivilegierten Agent-User des Basis-Images (uid 1000).
USER agent

# Basis-Image hat 2.1.280; "onFailure" braucht 2.1.295, Sicherheitsfixes 2.1.299.
ARG CLAUDE_CODE_VERSION=2.1.302
RUN claude install "${CLAUDE_CODE_VERSION}" && claude --version

# Jackson fuer den Hook vorwaermen: JBang laedt in der VM nicht ueber den Proxy.
# Version wie in GuardHook.java (//DEPS).
RUN set -euo pipefail; \
    printf '//DEPS com.fasterxml.jackson.core:jackson-databind:2.22.3\nclass Warm { public static void main(String[] a) {} }\n' > /tmp/Warm.java; \
    jbang /tmp/Warm.java; rm /tmp/Warm.java

RUN java -version && mvn -v && gradle --version && jbang --version
