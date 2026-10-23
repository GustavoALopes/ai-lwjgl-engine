package dev.aether.project;

import java.nio.file.Path;
import java.util.Objects;

/** A declaration for a future game-module loader; constructing it never loads code. */
public record GameModuleConfig(Path jar, String entryPoint) {
    public GameModuleConfig {
        jar = ProjectPaths.relative(jar, "gameModule.jar", true);
        Objects.requireNonNull(entryPoint, "entryPoint");
        if (!entryPoint.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*"))
            throw new IllegalArgumentException("gameModule.entryPoint must be a Java binary class name");
    }
}
