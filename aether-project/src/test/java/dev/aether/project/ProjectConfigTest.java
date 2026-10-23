package dev.aether.project;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProjectConfigTest {
    @Test void createsMinimalConfigurationAndRejectsUnsupportedVersions() {
        ProjectConfig config = ProjectConfig.create("Tetris", Path.of("assets"));
        assertEquals(1, config.formatVersion());
        assertTrue(config.startScene().isEmpty());
        assertTrue(config.gameModule().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new ProjectConfig(2, "Game",
                Path.of("assets"), Optional.empty(), Optional.empty()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "../assets", "assets/../../outside", "/assets",
            "C:/games/assets", "C:assets", "\\\\server\\share"})
    void rejectsInvalidProjectRelativePaths(String path) {
        assertThrows(IllegalArgumentException.class, () -> ProjectConfig.create("Game", Path.of(path)));
    }

    @Test void normalizesPathsAndSupportsWindowsSeparators() {
        ProjectConfig config = new ProjectConfig(1, "Game", Path.of("content/../assets"),
                Optional.of(Path.of("scenes\\main.aetherscene")),
                Optional.of(new GameModuleConfig(Path.of("build\\game.jar"), "games.Example$Module")));
        assertEquals(Path.of("assets"), config.assetDirectory());
        assertEquals(Optional.of(Path.of("scenes/main.aetherscene")), config.startScene());
        assertEquals(Path.of("build/game.jar"), config.gameModule().orElseThrow().jar());
    }

    @Test void rejectsInvalidOptionalFieldsAndNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> ProjectConfig.create(" ", Path.of("assets")));
        assertThrows(NullPointerException.class, () -> ProjectConfig.create(null, Path.of("assets")));
        assertThrows(NullPointerException.class, () -> ProjectConfig.create("Game", null));
        assertThrows(NullPointerException.class, () -> new ProjectConfig(1, "Game", Path.of("assets"),
                null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new ProjectConfig(1, "Game", Path.of("assets"),
                Optional.empty(), null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectConfig(1, "Game", Path.of("assets"),
                Optional.of(Path.of("../main.aetherscene")), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ProjectConfig(1, "Game", Path.of("assets"),
                Optional.of(Path.of(".")), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new GameModuleConfig(Path.of(".."), "games.Main"));
        assertThrows(IllegalArgumentException.class, () -> new GameModuleConfig(Path.of("."), "games.Main"));
        assertThrows(IllegalArgumentException.class, () -> new GameModuleConfig(Path.of("game.jar"), "bad class"));
        assertThrows(NullPointerException.class, () -> new GameModuleConfig(Path.of("game.jar"), null));
    }

    @Test void manifestRequiresCorrectExtensionAndConfiguration() {
        ProjectConfig config = ProjectConfig.create("Game", Path.of("."));
        assertThrows(IllegalArgumentException.class, () -> new ProjectFile(Path.of("game.json"), config));
        assertThrows(NullPointerException.class, () -> new ProjectFile(null, config));
        assertThrows(NullPointerException.class, () -> new ProjectFile(Path.of("game.aetherproj"), null));
    }
}
