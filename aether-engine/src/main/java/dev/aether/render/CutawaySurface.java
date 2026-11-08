package dev.aether.render;

import dev.aether.ecs.Component;
import dev.aether.math.Vec3;
import java.util.Objects;

/** Solid box wall shortened for a cutaway view. Height is in world units.
 * A world-space outward normal selects camera-facing room walls; ZERO uses
 * target obstruction instead. Simulation Transform and collision stay intact.
 */
public record CutawaySurface(double remainingHeight, Vec3 outwardNormal) implements Component {
    public CutawaySurface(double remainingHeight) {
        this(remainingHeight, Vec3.ZERO);
    }

    public CutawaySurface {
        if (!Double.isFinite(remainingHeight) || remainingHeight < 0)
            throw new IllegalArgumentException("remainingHeight must be finite and non-negative");
        Objects.requireNonNull(outwardNormal, "outwardNormal");
        if (!Double.isFinite(outwardNormal.x()) || !Double.isFinite(outwardNormal.y()) || !Double.isFinite(outwardNormal.z()))
            throw new IllegalArgumentException("outwardNormal must be finite");
    }
}
