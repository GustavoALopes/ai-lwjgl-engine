package dev.aether;

import dev.aether.project.ProjectSerializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TetrisProjectTest {
    @Test void exampleManifestMatchesTheTetrisProjectLayout() throws IOException {
        var project = new ProjectSerializer().read(Path.of("Tetris.aetherproj"));
        assertEquals("Tetris", project.config().name());
        assertTrue(Files.isDirectory(project.assetDirectory()));
        assertEquals(project.directory().resolve("assets"), project.assetDirectory());
        assertTrue(project.startScene().isEmpty());
        assertEquals(project.directory().resolve("target/aether-tetris.jar"),
                project.gameModuleJar().orElseThrow());
        assertEquals("dev.aether.demo.TetrisModule", project.config().gameModule().orElseThrow().entryPoint());
    }
}
