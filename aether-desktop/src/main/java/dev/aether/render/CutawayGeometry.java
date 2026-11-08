package dev.aether.render;

import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Draw-only box shortening keeps a closed, opaque top and a stationary base. */
public final class CutawayGeometry {
    private CutawayGeometry() { }

    public static Transform lowered(Transform original, double remainingHeight, double progress) {
        Objects.requireNonNull(original, "original");
        if (!Double.isFinite(remainingHeight) || remainingHeight < 0
                || !Double.isFinite(progress) || progress < 0 || progress > 1)
            throw new IllegalArgumentException("Height must be non-negative and progress within [0, 1]");
        double half = Math.abs(original.scale().y());
        double loweredHalf = half + (Math.min(half, remainingHeight / 2) - half) * progress;
        var r = original.rotationDegrees();
        Vector3f offset = new Matrix4f().rotateXYZ((float) Math.toRadians(r.x()),
                (float) Math.toRadians(r.y()), (float) Math.toRadians(r.z()))
                .transformDirection(new Vector3f(0, (float) (Math.copySign(1, original.scale().y()) * (loweredHalf - half)), 0));
        return new Transform(original.position().add(new Vec3(offset.x, offset.y, offset.z)), r,
                new Vec3(original.scale().x(), Math.copySign(loweredHalf, original.scale().y()), original.scale().z()));
    }
}
