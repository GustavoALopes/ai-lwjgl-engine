package dev.aether.editor;

import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import dev.aether.editor.TransformControls.TransformMode;
import imgui.ImGui;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real native ImGui mouse gestures without creating a window or OpenGL context. */
class TransformControlsTest {
    private Transform transform;
    private float fieldsY;
    private boolean scaleMode;

    @BeforeEach void initialize() {
        ImGui.createContext();
        ImGui.getIO().setIniFilename(null);
        ImGui.getIO().setDisplaySize(600, 400);
        ImGui.getIO().setDeltaTime(1f / 60);
        ImGui.getIO().getFonts().build();
        transform = Transform.identity().withPosition(new Vec3(0, 0, 0));
    }
    @AfterEach void close() { ImGui.destroyContext(); }

    private void fieldsFrame(float x, float y, boolean down) {
        ImGui.getIO().setMousePos(x, y);
        ImGui.getIO().setMouseDown(0, down);
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(300, 200);
        ImGui.begin("Inspector", ImGuiWindowFlags.NoDecoration);
        if (ImGui.button("Move##mode")) scaleMode = false;
        ImGui.sameLine();
        if (ImGui.button("Scale##mode")) scaleMode = true;
        fieldsY = ImGui.getCursorScreenPosY();
        transform = TransformControls.fields(transform);
        ImGui.end();
        ImGui.render();
    }

    @Test void inspectorScaleDoesNotCollideWithModeButtonAndPositionPersists() {
        fieldsFrame(-100, -100, false);
        float scaleY = fieldsY + 2 * ImGui.getFrameHeightWithSpacing() + 8;
        fieldsFrame(20, scaleY, false);
        fieldsFrame(20, scaleY, true);
        fieldsFrame(60, scaleY, true);
        fieldsFrame(60, scaleY, false);
        assertTrue(transform.scale().x() > 1, "Dragging Scale X must change scale");
        assertFalse(scaleMode, "Dragging the field must not activate the Scale mode button");
        Vec3 scale = transform.scale();
        float positionY = fieldsY + 8;
        fieldsFrame(20, positionY, false);
        fieldsFrame(20, positionY, true);
        fieldsFrame(60, positionY, true);
        fieldsFrame(60, positionY, false);
        assertTrue(transform.position().x() > 0, "Dragging Position X must change position");
        assertEquals(scale, transform.scale());
    }

    private TransformControls.GizmoResult gizmoFrame(float x, float y, boolean down, boolean scale, boolean enabled) {
        return gizmoFrame(x, y, down, scale ? TransformMode.SCALE : TransformMode.TRANSLATE, enabled);
    }

    private TransformControls.GizmoResult gizmoFrame(float x, float y, boolean down, TransformMode mode, boolean enabled) {
        ImGui.getIO().setMousePos(x, y);
        ImGui.getIO().setMouseDown(0, down);
        ImGui.newFrame();
        ImGuizmo.beginFrame();
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(600, 400);
        ImGui.begin("Scene View", ImGuiWindowFlags.NoDecoration);
        ImGui.dummy(584, 384);
        var view = new Matrix4f().lookAt(0, 0, 10, 0, 0, 0, 0, 1, 0);
        var projection = new Matrix4f().perspective((float) Math.toRadians(55), 1.5f, .1f, 100);
        var result = TransformControls.gizmo(transform, view, projection, 0, 0, 600, 400,
                mode, false, enabled);
        transform = result.transform();
        ImGui.end();
        ImGui.render();
        return result;
    }

    @Test void translatesAndScalesWithTheActiveCameraProjection() {
        gizmoFrame(-100, -100, false, false, true);
        var hover = gizmoFrame(340, 200, false, false, true);
        assertTrue(hover.capturesPointer(), "X handle must capture selection before scene picking");
        gizmoFrame(340, 200, true, false, true);
        gizmoFrame(380, 200, true, false, true);
        gizmoFrame(380, 200, false, false, true);
        assertTrue(transform.position().x() > .5);
        assertEquals(new Vec3(1, 1, 1), transform.scale());
        transform = Transform.identity();
        gizmoFrame(340, 200, false, true, true);
        gizmoFrame(340, 200, true, true, true);
        gizmoFrame(380, 200, true, true, true);
        gizmoFrame(380, 200, false, true, true);
        assertTrue(transform.scale().x() > 1.1, "Scale handle must resize model");
        assertEquals(Vec3.ZERO, transform.position());
    }

    @Test void disabledGizmoDoesNotReactToPointerInOtherPanels() {
        gizmoFrame(340, 200, false, false, false);
        gizmoFrame(340, 200, true, false, false);
        var result = gizmoFrame(380, 200, true, false, false);
        assertFalse(result.capturesPointer());
        assertEquals(Transform.identity(), transform);
        gizmoFrame(380, 200, false, false, false);
    }

    @Test void inspectorRotationEditsDegreesAndPreservesPositionAndScale() {
        transform = new Transform(new Vec3(2, 3, 4), Vec3.ZERO, new Vec3(-2, 3, 4));
        fieldsFrame(-100, -100, false);
        float rotationY = fieldsY + ImGui.getFrameHeightWithSpacing() + 8;
        fieldsFrame(20, rotationY, false);
        fieldsFrame(20, rotationY, true);
        fieldsFrame(60, rotationY, true);
        fieldsFrame(60, rotationY, false);
        assertTrue(transform.rotationDegrees().x() > 0);
        assertEquals(new Vec3(2, 3, 4), transform.position());
        assertEquals(new Vec3(-2, 3, 4), transform.scale());
    }

    @Test void rotationRingChangesOrientationAndPreservesMirroredScale() {
        transform = new Transform(Vec3.ZERO, new Vec3(20, 30, 10), new Vec3(-2, 3, 4));
        Vec3 originalRotation = transform.rotationDegrees();
        gizmoFrame(-100, -100, false, TransformMode.ROTATE, true);
        // Find a visible ring on a diagonal, away from the overlapping axis handles.
        float ringOffset = 0;
        for (int offset = 20; offset <= 100; offset++) {
            if (gizmoFrame(300 + offset, 200 - offset, false, TransformMode.ROTATE, true).capturesPointer()) {
                ringOffset = offset;
                break;
            }
        }
        assertTrue(ringOffset > 0, "Rotation ring must capture the pointer");
        gizmoFrame(300 + ringOffset, 200 - ringOffset, true, TransformMode.ROTATE, true);
        gizmoFrame(300 + ringOffset, 200 + ringOffset, true, TransformMode.ROTATE, true);
        gizmoFrame(300 + ringOffset, 200 + ringOffset, false, TransformMode.ROTATE, true);
        assertNotEquals(originalRotation, transform.rotationDegrees());
        assertEquals(Vec3.ZERO, transform.position());
        assertEquals(new Vec3(-2, 3, 4), transform.scale());
    }

    @Test void disabledRotationDoesNotEditTheObject() {
        Transform original = transform;
        gizmoFrame(340, 160, false, TransformMode.ROTATE, false);
        gizmoFrame(340, 160, true, TransformMode.ROTATE, false);
        assertFalse(gizmoFrame(340, 240, true, TransformMode.ROTATE, false).capturesPointer());
        gizmoFrame(340, 240, false, TransformMode.ROTATE, false);
        assertEquals(original, transform);
    }
}
