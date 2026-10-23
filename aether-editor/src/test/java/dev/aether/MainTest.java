package dev.aether;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainTest {
    @Test void headlessEntryPointRunsWithoutOpeningADesktopContext() {
        assertDoesNotThrow(() -> Main.main(new String[]{"--headless"}));
    }
}
