package dev.aether.render;

import dev.aether.ecs.Component;
import java.util.Objects;

/**
 * Renderer-facing mesh/material reference. Imported geometry is supplied by a
 * ModelMesh component on the same entity; primitive identifiers need no geometry component.
 */
public record MeshRenderer(String meshId, String materialId) implements Component {
    public MeshRenderer {
        Objects.requireNonNull(meshId);
        Objects.requireNonNull(materialId);
    }
}
