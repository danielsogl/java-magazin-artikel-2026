# Listing 6: Java-Template fuer Docker Sandboxes (sbx).
#
# Baut auf dem offiziellen minimalen Claude-Code-Template auf und legt eine
# reproduzierbare Java-Toolchain dazu: Temurin 25 (JDK), Maven 3.10.0,
# Gradle 9.8.1. Jeder Download wird gegen die offizielle Checksumme geprueft
# -- ein manipuliertes Artefakt bricht den Build ab.
#
# Bauen (lokal, arch der Host-microVM):
#   docker build -t jm-java-sbx:latest docker/
# Starten:
#   sbx run claude --template jm-java-sbx:latest .
FROM docker/sandbox-templates:claude-code-minimal

# TARGETARCH wird von buildx gesetzt: amd64 | arm64
ARG TARGETARCH

ARG JDK_VERSION=25.0.4+7
ARG MAVEN_VERSION=3.10.0
ARG GRADLE_VERSION=9.8.1

# Checksummen (offiziell, siehe docs/e2e-ergebnisse.md fuer die Quellen)
ARG JDK_SHA256_amd64=e58fcdcd637b25c03ca84cbbcefc70d11efb8f4b4cbd05decc9f661769d77f94
ARG JDK_SHA256_arm64=621f7196f0b682fb557da58bec89bd7dfe5419811fe1c0ba75c9cc8432f084c7
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

ENV JAVA_HOME=/opt/java \
    MAVEN_HOME=/opt/maven \
    GRADLE_HOME=/opt/gradle \
    PATH=/opt/java/bin:/opt/maven/bin:/opt/gradle/bin:$PATH

# Optional: organisationsweite, nicht ueberschreibbare Managed Settings.
# Aktivieren, indem die Datei nach docker/ gelegt und die Zeile entkommentiert wird.
# COPY managed-settings.json /etc/claude-code/managed-settings.json

# Zurueck auf den unprivilegierten Agent-User des Basis-Images (uid 1000).
USER agent
RUN java -version && mvn -v && gradle --version
