package dev.aether.render;

import dev.aether.ecs.Component;
import java.util.Objects;

/**
 * Renderer-facing mesh/material reference. The Assimp import pipeline resolves
 * these identifiers.
 */
public record MeshRenderer(String meshId, String materialId) implements Component {
    public MeshRenderer {
        Objects.requireNonNull(meshId);
        Objects.requireNonNull(materialId);
    }
}
