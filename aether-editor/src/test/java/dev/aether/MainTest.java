package dev.aether;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary;

    @Test void headlessProjectLaunchValidatesRealManifestsAndReportsFailures() throws java.io.IOException {
        java.nio.file.Files.createDirectory(temporary.resolve("assets"));
        var file = temporary.resolve("Game.aetherproj");
        new dev.aether.project.ProjectSerializer().write(file,
                dev.aether.project.ProjectConfig.create("Game", java.nio.file.Path.of("assets")));
        assertDoesNotThrow(() -> Main.main(new String[]{"--headless", file.toString()}));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> Main.main(new String[]{"--headless", temporary.resolve("Missing.aetherproj").toString()}));
    }
    @Test void headlessEntryPointRunsWithoutOpeningADesktopContext() {
        assertDoesNotThrow(() -> Main.main(new String[]{"--headless"}));
    }
}
