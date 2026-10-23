package dev.aether.project;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Strict UTF-8 manifest persistence. Referenced assets and code are not opened or executed. */
public final class ProjectSerializer {
    public ProjectFile read(Path file) throws IOException {
        Path manifest = ProjectPaths.manifest(file);
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(manifest, StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            try {
                ProjectConfig config = readConfig(reader);
                if (reader.peek() != JsonToken.END_DOCUMENT)
                    throw new IllegalArgumentException("Unexpected content after the project object");
                return new ProjectFile(manifest, config);
            } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
                throw new ProjectFormatException(manifest, exception);
            }
        }
    }

    /**
     * Replaces a manifest after writing it completely to a sibling temporary file.
     * Uses an atomic move where the filesystem supports it. Parent directories must already exist.
     */
    public ProjectFile write(Path file, ProjectConfig config) throws IOException {
        ProjectFile project = new ProjectFile(file, Objects.requireNonNull(config, "config"));
        String json = encode(config);
        Path temporary = Files.createTempFile(project.directory(), ".aether-project-", ".tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, project.manifestFile(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, project.manifestFile(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return project;
    }

    private ProjectConfig readConfig(JsonReader reader) throws IOException {
        Integer version = null;
        String name = null;
        Path assets = null;
        Optional<Path> scene = Optional.empty();
        Optional<GameModuleConfig> module = Optional.empty();
        Set<String> fields = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String field = nextField(reader, fields);
            switch (field) {
                case "formatVersion" -> {
                    if (reader.peek() != JsonToken.NUMBER)
                        throw new IllegalArgumentException("formatVersion must be an integer");
                    String value = reader.nextString();
                    if (!value.matches("-?(0|[1-9][0-9]*)"))
                        throw new IllegalArgumentException("formatVersion must be an integer");
                    version = Integer.valueOf(value);
                }
                case "name" -> name = string(reader, field);
                case "assetDirectory" -> assets = Path.of(string(reader, field));
                case "startScene" -> scene = Optional.of(Path.of(string(reader, field)));
                case "gameModule" -> module = Optional.of(readModule(reader));
                default -> throw new IllegalArgumentException("Unknown project field: " + field);
            }
        }
        reader.endObject();
        return new ProjectConfig(required(version, "formatVersion"), required(name, "name"),
                required(assets, "assetDirectory"), scene, module);
    }

    private GameModuleConfig readModule(JsonReader reader) throws IOException {
        Path jar = null;
        String entryPoint = null;
        Set<String> fields = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String field = nextField(reader, fields);
            switch (field) {
                case "jar" -> jar = Path.of(string(reader, "gameModule.jar"));
                case "entryPoint" -> entryPoint = string(reader, "gameModule.entryPoint");
                default -> throw new IllegalArgumentException("Unknown gameModule field: " + field);
            }
        }
        reader.endObject();
        return new GameModuleConfig(required(jar, "gameModule.jar"),
                required(entryPoint, "gameModule.entryPoint"));
    }

    private String nextField(JsonReader reader, Set<String> fields) throws IOException {
        String field = reader.nextName();
        if (!fields.add(field))
            throw new IllegalArgumentException("Duplicate field: " + field);
        return field;
    }

    private String string(JsonReader reader, String field) throws IOException {
        if (reader.peek() != JsonToken.STRING)
            throw new IllegalArgumentException(field + " must be a string");
        return reader.nextString();
    }

    private <T> T required(T value, String field) {
        if (value == null)
            throw new IllegalArgumentException("Missing required field: " + field);
        return value;
    }

    private String encode(ProjectConfig config) throws IOException {
        StringWriter output = new StringWriter();
        try (JsonWriter writer = new JsonWriter(output)) {
            writer.setStrictness(Strictness.STRICT);
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("formatVersion").value(config.formatVersion());
            writer.name("name").value(config.name());
            writer.name("assetDirectory").value(ProjectPaths.portable(config.assetDirectory()));
            if (config.startScene().isPresent())
                writer.name("startScene").value(ProjectPaths.portable(config.startScene().orElseThrow()));
            if (config.gameModule().isPresent()) {
                GameModuleConfig module = config.gameModule().orElseThrow();
                writer.name("gameModule").beginObject();
                writer.name("jar").value(ProjectPaths.portable(module.jar()));
                writer.name("entryPoint").value(module.entryPoint());
                writer.endObject();
            }
            writer.endObject();
        }
        return output + "\n";
    }
}
