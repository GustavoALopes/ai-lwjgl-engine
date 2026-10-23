package dev.aether.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSerializerTest {
    @Test void createsManifestsWithoutOverwritingExistingContent() throws IOException {
        Path file = temporary.resolve("New.aetherproj");
        var config = ProjectConfig.create("New", Path.of("assets"));
        serializer.create(file, config);
        String original = Files.readString(file);
        assertEquals(config, serializer.read(file).config());
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> serializer.create(file, ProjectConfig.create("Replacement", Path.of("assets"))));
        assertEquals(original, Files.readString(file));
        try (var files = Files.list(temporary)) { assertEquals(1, files.count()); }
    }
    @TempDir Path temporary;
    private final ProjectSerializer serializer = new ProjectSerializer();
    private static final String MINIMAL =
            "{\"formatVersion\":1,\"name\":\"Tetris\",\"assetDirectory\":\"assets\"}";

    @Test void readsMinimalManifestWithoutRequiringReferencedFiles() throws IOException {
        Path file = temporary.resolve("Tetris.aetherproj");
        Files.writeString(file, MINIMAL);
        ProjectFile project = serializer.read(file);
        assertEquals(file.toAbsolutePath(), project.manifestFile());
        assertEquals("Tetris", project.config().name());
        assertEquals(temporary.resolve("assets"), project.assetDirectory());
        assertTrue(project.startScene().isEmpty());
        assertTrue(project.gameModuleJar().isEmpty());
        assertFalse(Files.exists(project.assetDirectory()));
    }

    @Test void roundTripsUnicodeEscapesAndAllOptionalFields() throws IOException {
        ProjectConfig config = new ProjectConfig(1, "Tétris \"test\"\n新",
                Path.of("assets"), Optional.of(Path.of("scenes/main.aetherscene")),
                Optional.of(new GameModuleConfig(Path.of("target/game.jar"), "games.tetris.Module")));
        Path file = temporary.resolve("Game.aetherproj");
        ProjectFile written = serializer.write(file, config);
        ProjectFile read = serializer.read(file);
        assertEquals(written, read);
        assertEquals(config, read.config());
        assertEquals(Optional.of(temporary.resolve("scenes/main.aetherscene")), read.startScene());
        assertEquals(Optional.of(temporary.resolve("target/game.jar")), read.gameModuleJar());
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("Tétris"));
    }

    @Test void relocationResolvesEverythingAgainstTheNewManifestDirectory() throws IOException {
        ProjectConfig config = new ProjectConfig(1, "Game", Path.of("assets"),
                Optional.of(Path.of("scenes/main.aetherscene")),
                Optional.of(new GameModuleConfig(Path.of("build/game.jar"), "games.Main")));
        Path first = Files.createDirectory(temporary.resolve("first"));
        Path second = Files.createDirectory(temporary.resolve("second"));
        serializer.write(first.resolve("Game.aetherproj"), config);
        Files.copy(first.resolve("Game.aetherproj"), second.resolve("Game.aetherproj"));
        ProjectFile project = serializer.read(second.resolve("Game.aetherproj"));
        assertEquals(config, project.config());
        assertEquals(second.resolve("assets"), project.assetDirectory());
        assertEquals(Optional.of(second.resolve("scenes/main.aetherscene")), project.startScene());
        assertEquals(Optional.of(second.resolve("build/game.jar")), project.gameModuleJar());
    }

    @Test void writesStablePortableJsonAndReplacesAnExistingManifest() throws IOException {
        Path file = temporary.resolve("Game.aetherproj");
        Files.writeString(file, "old contents");
        ProjectConfig config = ProjectConfig.create("Game", Path.of("content\\assets"));
        serializer.write(file, config);
        String first = Files.readString(file);
        serializer.write(file, config);
        assertEquals(first, Files.readString(file));
        assertTrue(first.contains("\"assetDirectory\": \"content/assets\""));
        assertFalse(first.contains("startScene"));
        assertFalse(first.contains("gameModule"));
        assertEquals(config, serializer.read(file).config());
        try (var files = Files.list(temporary)) {
            assertEquals(1, files.count(), "Temporary save files must be cleaned up");
        }
    }

    @Test void allowsAnExplicitProjectRootAssetDirectory() throws IOException {
        Path file = temporary.resolve("Root.aetherproj");
        serializer.write(file, ProjectConfig.create("Root", Path.of(".")));
        assertEquals(temporary, serializer.read(file).assetDirectory());
        assertTrue(Files.readString(file).contains("\"assetDirectory\": \".\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "{}", "{", "{name:'bad'}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\"} {}",
            "{\"formatVersion\":2,\"name\":\"Game\",\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1.5,\"name\":\"Game\",\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1.0,\"name\":\"Game\",\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":\"1\",\"name\":\"Game\",\"assetDirectory\":\"assets\"}",
            "{\"name\":\"Game\",\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1,\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1,\"name\":\"Game\"}",
            "{\"formatVersion\":1,\"name\":\" \",\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1,\"name\":123,\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1,\"name\":null,\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"../outside\"}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",\"startScene\":null}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"name\":\"Other\",\"assetDirectory\":\"assets\"}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",\"typo\":true}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",\"gameModule\":{}}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",\"gameModule\":null}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",\"gameModule\":{\"jar\":\"a.jar\",\"entryPoint\":\"games.Main\",\"jar\":\"b.jar\"}}",
            "{\"formatVersion\":1,\"name\":\"Game\",\"assetDirectory\":\"assets\",\"gameModule\":{\"jar\":\"a.jar\",\"entryPoint\":\"games.Main\",\"typo\":1}}"
    })
    void rejectsMalformedOrInvalidManifestsWithFileContext(String json) throws IOException {
        Path file = temporary.resolve("Invalid.aetherproj");
        Files.writeString(file, json);
        ProjectFormatException exception = assertThrows(ProjectFormatException.class, () -> serializer.read(file));
        assertTrue(exception.getMessage().contains(file.toString()));
        assertNotNull(exception.getCause());
        assertEquals(json, Files.readString(file), "Reading must never modify the manifest");
    }

    @Test void acceptsUnorderedFieldsAndRetainsOptionalModuleDeclarations() throws IOException {
        Path file = temporary.resolve("Game.aetherproj");
        Files.writeString(file, """
                {
                  "gameModule": {"entryPoint": "games.Main", "jar": "build/game.jar"},
                  "startScene": "scenes/main.aetherscene",
                  "assetDirectory": "assets",
                  "name": "Game",
                  "formatVersion": 1
                }
                """);
        assertEquals("games.Main", serializer.read(file).config().gameModule().orElseThrow().entryPoint());
    }

    @Test void reportsMissingFilesAndDoesNotCreateMissingParentDirectories() {
        assertThrows(NoSuchFileException.class, () -> serializer.read(temporary.resolve("Missing.aetherproj")));
        assertThrows(IOException.class, () -> serializer.write(temporary.resolve("missing/Game.aetherproj"),
                ProjectConfig.create("Game", Path.of("assets"))));
        assertFalse(Files.exists(temporary.resolve("missing")));
    }

    @Test void rejectsInvalidWriteArgumentsBeforeTouchingExistingFiles() throws IOException {
        Path file = temporary.resolve("Game.json");
        Files.writeString(file, "preserved");
        assertThrows(IllegalArgumentException.class,
                () -> serializer.write(file, ProjectConfig.create("Game", Path.of("assets"))));
        assertThrows(NullPointerException.class, () -> serializer.write(file, null));
        assertEquals("preserved", Files.readString(file));
    }

    @Test void failedReplacementPreservesTheDestinationAndCleansTemporaryFiles() throws IOException {
        Path destination = Files.createDirectory(temporary.resolve("Game.aetherproj"));
        Path existing = Files.writeString(destination.resolve("existing.txt"), "preserved");
        assertThrows(IOException.class, () -> serializer.write(destination,
                ProjectConfig.create("Game", Path.of("assets"))));
        assertEquals("preserved", Files.readString(existing));
        try (var files = Files.list(temporary)) {
            assertEquals(1, files.count(), "A failed move must not leave a temporary save file");
        }
    }
}
