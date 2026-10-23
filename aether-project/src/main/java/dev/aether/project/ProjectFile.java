package dev.aether.project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** A validated manifest snapshot anchored to its own directory, never the process working directory. */
public record ProjectFile(Path manifestFile, ProjectConfig config) {
    public ProjectFile {
        manifestFile = ProjectPaths.manifest(manifestFile);
        Objects.requireNonNull(config, "config");
    }

    public Path directory() {
        return manifestFile.getParent();
    }

    public Path assetDirectory() {
        return directory().resolve(config.assetDirectory()).normalize();
    }

    public Optional<Path> startScene() {
        return config.startScene().map(path -> directory().resolve(path).normalize());
    }

    public Optional<Path> gameModuleJar() {
        return config.gameModule().map(module -> directory().resolve(module.jar()).normalize());
    }
}
