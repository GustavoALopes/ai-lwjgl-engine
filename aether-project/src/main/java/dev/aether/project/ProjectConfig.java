package dev.aether.project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Immutable on-disk settings, independent of the project directory and editor state. */
public record ProjectConfig(int formatVersion, String name, Path assetDirectory,
                            Optional<Path> startScene, Optional<GameModuleConfig> gameModule) {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public ProjectConfig {
        if (formatVersion != CURRENT_FORMAT_VERSION)
            throw new IllegalArgumentException("Unsupported project formatVersion: " + formatVersion
                    + "; supported version is " + CURRENT_FORMAT_VERSION);
        Objects.requireNonNull(name, "name");
        if (name.isBlank())
            throw new IllegalArgumentException("Project name must not be blank");
        assetDirectory = ProjectPaths.relative(assetDirectory, "assetDirectory", false);
        startScene = Objects.requireNonNull(startScene, "startScene")
                .map(path -> ProjectPaths.relative(path, "startScene", true));
        Objects.requireNonNull(gameModule, "gameModule");
    }

    public static ProjectConfig create(String name, Path assetDirectory) {
        return new ProjectConfig(CURRENT_FORMAT_VERSION, name, assetDirectory,
                Optional.empty(), Optional.empty());
    }
}
