package de.javamagazin.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GreetingControllerTest {

    @Test
    void greetingReturnsExpectedText() {
        assertThat(new GreetingController().greeting())
                .isEqualTo("Hallo aus dem Opfer-Workspace");
    }
}
