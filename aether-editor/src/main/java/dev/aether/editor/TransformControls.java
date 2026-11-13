package dev.aether.editor;

import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import imgui.ImGui;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;
import java.util.Arrays;
import org.joml.Matrix4f;

/** Shared inspector and viewport transform controls, independent of project/session ownership. */
final class TransformControls {
    private TransformControls() { }

    enum TransformMode {
        TRANSLATE("Move", Operation.TRANSLATE, Mode.WORLD),
        ROTATE("Rotate", Operation.ROTATE, Mode.LOCAL),
        SCALE("Scale", Operation.SCALE, Mode.LOCAL);

        final String label;
        final int operation;
        final int space;

        TransformMode(String label, int operation, int space) {
            this.label = label;
            this.operation = operation;
            this.space = space;
        }
    }

    static Transform fields(Transform transform) {
        float[] position = floats(transform.position());
        if (ImGui.dragFloat3("Position##transform", position, .05f))
            transform = transform.withPosition(vector(position));
        float[] rotation = floats(transform.rotationDegrees());
        if (ImGui.dragFloat3("Rotation (deg)##transform", rotation, .5f))
            transform = new Transform(transform.position(), vector(rotation), transform.scale());
        float[] scale = floats(transform.scale());
        if (ImGui.dragFloat3("Scale##transform", scale, .02f, .05f, 100f))
            transform = transform.withScale(vector(scale));
        return transform;
    }

    record GizmoResult(Transform transform, boolean capturesPointer) { }

    static GizmoResult gizmo(Transform transform, Matrix4f view, Matrix4f projection,
                            float left, float top, float width, float height,
                            TransformMode mode, boolean orthographic, boolean enabled) {
        if (width <= 0 || height <= 0) return new GizmoResult(transform, false);
        // Rotation rings only need orientation. Unit scale also supports mirrored or zero-scale objects.
        float[] matrix = matrix(mode == TransformMode.ROTATE
                ? transform.withScale(new Vec3(1, 1, 1)) : transform);
        float[] before = matrix.clone();
        ImGuizmo.setDrawList();
        ImGuizmo.setRect(left, top, width, height);
        ImGuizmo.setOrthographic(orthographic);
        ImGuizmo.setEnabled(enabled);
        ImGuizmo.manipulate(view.get(new float[16]), projection.get(new float[16]), matrix,
                mode.operation, mode.space);
        boolean captures = enabled && (ImGuizmo.isOver() || ImGuizmo.isUsing());
        if (enabled && ImGuizmo.isUsing() && !Arrays.equals(before, matrix)) {
            if (mode == TransformMode.ROTATE) {
                var radians = new Matrix4f().set(matrix).getNormalizedRotation(new org.joml.Quaternionf())
                        .getEulerAnglesXYZ(new org.joml.Vector3f());
                transform = new Transform(transform.position(),
                        new Vec3(Math.toDegrees(radians.x), Math.toDegrees(radians.y), Math.toDegrees(radians.z)),
                        transform.scale());
            } else if (mode == TransformMode.SCALE) {
                // Only scale is editable here. Preserve the existing rotation and position exactly.
                var decomposed = new Matrix4f().set(matrix).getScale(new org.joml.Vector3f());
                transform = transform.withScale(new Vec3(
                        signedScale(decomposed.x, transform.scale().x()),
                        signedScale(decomposed.y, transform.scale().y()),
                        signedScale(decomposed.z, transform.scale().z())));
            } else {
                transform = transform.withPosition(new Vec3(matrix[12], matrix[13], matrix[14]));
            }
        }
        return new GizmoResult(transform, captures);
    }

    static float[] matrix(Transform transform) {
        return new Matrix4f().translate((float) transform.position().x(), (float) transform.position().y(), (float) transform.position().z())
                .rotateXYZ((float) Math.toRadians(transform.rotationDegrees().x()),
                        (float) Math.toRadians(transform.rotationDegrees().y()), (float) Math.toRadians(transform.rotationDegrees().z()))
                .scale((float) transform.scale().x(), (float) transform.scale().y(), (float) transform.scale().z())
                .get(new float[16]);
    }

    private static double signedScale(float value, double original) {
        return Math.copySign(Math.max(.05f, value), original);
    }

    private static float[] floats(Vec3 vector) {
        return new float[] { (float) vector.x(), (float) vector.y(), (float) vector.z() };
    }
    private static Vec3 vector(float[] vector) { return new Vec3(vector[0], vector[1], vector[2]); }
}
