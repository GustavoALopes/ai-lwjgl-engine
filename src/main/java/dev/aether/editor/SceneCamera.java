package dev.aether.editor;

import dev.aether.math.Vec3;
import org.joml.Matrix4f;

import java.util.Objects;

/**
 * Camera used by the editor Scene View. It has no window or OpenGL dependency so
 * navigation behaviour can be exercised in headless tests.
 */
public final class SceneCamera {
    private static final double MIN_DISTANCE = 0.1;
    private static final double MAX_PITCH_RADIANS = Math.toRadians(89.0);
    private Vec3 position;
    private Vec3 target;

    public SceneCamera(Vec3 position, Vec3 target) {
        this.position = valid(position, "position");
        this.target = valid(target, "target");
        if (distance(position, target) < MIN_DISTANCE) {
            throw new IllegalArgumentException("position and target must be at least " + MIN_DISTANCE + " apart");
        }
    }

    public static SceneCamera defaultCamera() {
        return new SceneCamera(new Vec3(9, 7, 11), Vec3.ZERO);
    }

    public Vec3 position() { return position; }
    public Vec3 target() { return target; }
    public double focusDistance() { return distance(position, target); }

    /** Orbits around the current focus point, preserving the focus distance. */
    public void orbit(double yawDeltaRadians, double pitchDeltaRadians) {
        requireFinite(yawDeltaRadians, "yawDeltaRadians");
        requireFinite(pitchDeltaRadians, "pitchDeltaRadians");
        Vec3 offset = subtract(position, target);
        double radius = length(offset);
        double yaw = Math.atan2(offset.z(), offset.x()) + yawDeltaRadians;
        double pitch = Math.asin(offset.y() / radius);
        pitch = Math.clamp(pitch + pitchDeltaRadians, -MAX_PITCH_RADIANS, MAX_PITCH_RADIANS);
        double horizontal = radius * Math.cos(pitch);
        position = add(target, new Vec3(horizontal * Math.cos(yaw), radius * Math.sin(pitch), horizontal * Math.sin(yaw)));
    }

    /** Pans in camera-local right/up units while keeping the same viewing direction. */
    public void pan(double rightAmount, double upAmount) {
        requireFinite(rightAmount, "rightAmount");
        requireFinite(upAmount, "upAmount");
        Vec3 forward = normalize(subtract(target, position));
        Vec3 right = normalize(cross(forward, new Vec3(0, 1, 0)));
        Vec3 up = normalize(cross(right, forward));
        translate(add(multiply(right, rightAmount), multiply(up, upAmount)));
    }

    /** Pans an orthographic XY scene view without changing its fixed orientation. */
    public void panOrthographic(double rightAmount, double upAmount) {
        requireFinite(rightAmount, "rightAmount");
        requireFinite(upAmount, "upAmount");
        translate(new Vec3(rightAmount, upAmount, 0));
    }

    /** Moves toward/away from the focus point without crossing it. */
    public void dolly(double amount) {
        requireFinite(amount, "amount");
        Vec3 forward = normalize(subtract(target, position));
        double permitted = Math.max(0, distance(position, target) - MIN_DISTANCE);
        position = add(position, multiply(forward, Math.min(amount, permitted)));
    }

    /** Moves the camera and its focus together, as in a first-person fly mode. */
    public void fly(double rightAmount, double upAmount, double forwardAmount) {
        requireFinite(rightAmount, "rightAmount");
        requireFinite(upAmount, "upAmount");
        requireFinite(forwardAmount, "forwardAmount");
        Vec3 forward = normalize(subtract(target, position));
        Vec3 right = normalize(cross(forward, new Vec3(0, 1, 0)));
        translate(add(add(multiply(right, rightAmount), new Vec3(0, upAmount, 0)), multiply(forward, forwardAmount)));
    }

    public Matrix4f viewMatrix() {
        return new Matrix4f().lookAt((float) position.x(), (float) position.y(), (float) position.z(),
            (float) target.x(), (float) target.y(), (float) target.z(), 0, 1, 0);
    }

    /** Returns an XY editor view looking straight down the negative Z axis. */
    public Matrix4f orthographicViewMatrix() {
        double distance = focusDistance();
        return new Matrix4f().lookAt((float) target.x(), (float) target.y(), (float) (target.z() + distance),
            (float) target.x(), (float) target.y(), (float) target.z(), 0, 1, 0);
    }

    private void translate(Vec3 delta) { position = add(position, delta); target = add(target, delta); }
    private static Vec3 valid(Vec3 value, String name) { Objects.requireNonNull(value, name); requireFinite(value.x(), name + ".x"); requireFinite(value.y(), name + ".y"); requireFinite(value.z(), name + ".z"); return value; }
    private static void requireFinite(double value, String name) { if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite"); }
    private static Vec3 add(Vec3 a, Vec3 b) { return new Vec3(a.x() + b.x(), a.y() + b.y(), a.z() + b.z()); }
    private static Vec3 subtract(Vec3 a, Vec3 b) { return new Vec3(a.x() - b.x(), a.y() - b.y(), a.z() - b.z()); }
    private static Vec3 multiply(Vec3 value, double scalar) { return new Vec3(value.x() * scalar, value.y() * scalar, value.z() * scalar); }
    private static double distance(Vec3 a, Vec3 b) { return length(subtract(a, b)); }
    private static double length(Vec3 value) { return Math.sqrt(value.x() * value.x() + value.y() * value.y() + value.z() * value.z()); }
    private static Vec3 normalize(Vec3 value) { double length = length(value); return multiply(value, 1.0 / length); }
    private static Vec3 cross(Vec3 a, Vec3 b) { return new Vec3(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.x()); }
}
