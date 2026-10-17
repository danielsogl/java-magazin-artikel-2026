package de.javamagazin.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Beweist, dass Testcontainers gegen den Docker-Daemon in der sbx-microVM läuft.
 * Überspringt sich, wenn kein Docker erreichbar ist. So bleibt `mvn test`
 * auch ohne Docker (und auf dem claude-code-minimal-Template) grün.
 */
@Testcontainers
class ContainerSmokeTest {

    @Test
    void startetContainerGegenDenDockerDaemon() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "kein Docker-Daemon erreichbar, Test wird uebersprungen");

        try (GenericContainer<?> container =
                new GenericContainer<>(DockerImageName.parse("alpine:3.20"))
                        .withCommand("sh", "-c", "echo java-magazin && sleep 30")) {
            container.start();
            assertThat(container.isRunning()).isTrue();
        }
    }
}
