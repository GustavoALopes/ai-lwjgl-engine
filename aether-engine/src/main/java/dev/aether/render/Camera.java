package dev.aether.render;

import dev.aether.ecs.Component;
import java.util.Objects;

/** Camera optics. Transform supplies pose; forward is local -Z and up is +Y.
 * The first active camera with a Transform in entity registration order is used.
 */
public record Camera(Projection projection, double fieldOfViewDegrees, double orthographicHeight,
                     double nearClip, double farClip, boolean active) implements Component {
    public enum Projection { PERSPECTIVE, ORTHOGRAPHIC }

    public Camera {
        Objects.requireNonNull(projection, "projection");
        if (!Double.isFinite(fieldOfViewDegrees) || fieldOfViewDegrees <= 0 || fieldOfViewDegrees >= 180
                || !Double.isFinite(orthographicHeight) || orthographicHeight <= 0
                || !Double.isFinite(nearClip) || nearClip <= 0
                || !Double.isFinite(farClip) || farClip <= nearClip)
            throw new IllegalArgumentException("Camera optics must be finite with valid field of view and clip range");
    }

    public static Camera perspective(double fieldOfViewDegrees, double nearClip, double farClip) {
        return new Camera(Projection.PERSPECTIVE, fieldOfViewDegrees, 10, nearClip, farClip, true);
    }
}
