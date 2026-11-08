package dev.aether.render;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.scene.Transform;
import java.util.Objects;
import java.util.Optional;
import org.joml.Matrix4f;

/** CPU-only conversion of engine camera declarations to rendering matrices. */
public final class CameraMatrices {
    private CameraMatrices() { }

    public record Frame(Entity entity, Matrix4f projection, Matrix4f view) {
        public Frame {
            Objects.requireNonNull(entity, "entity");
            projection = new Matrix4f(projection);
            view = new Matrix4f(view);
        }
        @Override public Matrix4f projection() { return new Matrix4f(projection); }
        @Override public Matrix4f view() { return new Matrix4f(view); }
    }

    public static Optional<Frame> active(World world, float aspect) {
        Objects.requireNonNull(world, "world");
        if (!Float.isFinite(aspect) || aspect <= 0)
            throw new IllegalArgumentException("aspect must be finite and positive");
        for (var entity : world.entities()) {
            Camera camera = world.get(entity, Camera.class).orElse(null);
            Transform transform = world.get(entity, Transform.class).orElse(null);
            if (camera == null || !camera.active() || transform == null) continue;
            var p = transform.position();
            var r = transform.rotationDegrees();
            // Camera scale never affects the viewing frustum.
            Matrix4f view = new Matrix4f().translate((float) p.x(), (float) p.y(), (float) p.z())
                    .rotateXYZ((float) Math.toRadians(r.x()), (float) Math.toRadians(r.y()),
                            (float) Math.toRadians(r.z())).invert();
            Matrix4f projection;
            if (camera.projection() == Camera.Projection.PERSPECTIVE)
                projection = new Matrix4f().perspective((float) Math.toRadians(camera.fieldOfViewDegrees()),
                        aspect, (float) camera.nearClip(), (float) camera.farClip());
            else {
                float half = (float) camera.orthographicHeight() / 2;
                projection = new Matrix4f().ortho(-half * aspect, half * aspect, -half, half,
                        (float) camera.nearClip(), (float) camera.farClip());
            }
            return Optional.of(new Frame(entity, projection, view));
        }
        return Optional.empty();
    }
}
