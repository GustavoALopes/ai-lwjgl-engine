package dev.aether.render;

import dev.aether.ecs.Component;
import dev.aether.math.Vec3;
import java.util.Objects;

/** Declarative linear RGB tint and emissive intensity; HDR color values may exceed one. */
public record Material(Vec3 color, double emissive) implements Component {
    public Material {
        Objects.requireNonNull(color, "color");
        if (!Double.isFinite(color.x()) || !Double.isFinite(color.y()) || !Double.isFinite(color.z())
                || color.x() < 0 || color.y() < 0 || color.z() < 0
                || !Double.isFinite(emissive) || emissive < 0)
            throw new IllegalArgumentException("Material values must be finite and non-negative");
    }
}
