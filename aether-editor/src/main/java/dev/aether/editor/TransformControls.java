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

    static Transform fields(Transform transform) {
        float[] position = floats(transform.position());
        if (ImGui.dragFloat3("Position##transform", position, .05f))
            transform = transform.withPosition(vector(position));
        float[] scale = floats(transform.scale());
        if (ImGui.dragFloat3("Scale##transform", scale, .02f, .05f, 100f))
            transform = transform.withScale(vector(scale));
        return transform;
    }

    record GizmoResult(Transform transform, boolean capturesPointer) { }

    static GizmoResult gizmo(Transform transform, Matrix4f view, Matrix4f projection,
                            float left, float top, float width, float height,
                            boolean scale, boolean orthographic, boolean enabled) {
        if (width <= 0 || height <= 0) return new GizmoResult(transform, false);
        float[] matrix = matrix(transform);
        float[] before = matrix.clone();
        ImGuizmo.setDrawList();
        ImGuizmo.setRect(left, top, width, height);
        ImGuizmo.setOrthographic(orthographic);
        ImGuizmo.setEnabled(enabled);
        ImGuizmo.manipulate(view.get(new float[16]), projection.get(new float[16]), matrix,
                scale ? Operation.SCALE : Operation.TRANSLATE, scale ? Mode.LOCAL : Mode.WORLD);
        boolean captures = enabled && (ImGuizmo.isOver() || ImGuizmo.isUsing());
        if (enabled && ImGuizmo.isUsing() && !Arrays.equals(before, matrix)) {
            if (scale) {
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
