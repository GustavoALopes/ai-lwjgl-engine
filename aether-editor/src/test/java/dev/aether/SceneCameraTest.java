package dev.aether;

import dev.aether.editor.SceneCamera;
import dev.aether.math.Vec3;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

class SceneCameraTest {
    @Test void orbitPreservesFocusAndDistance() {
        SceneCamera camera = new SceneCamera(new Vec3(0, 0, 10), Vec3.ZERO);
        camera.orbit(Math.PI / 2, 0);
        assertEquals(Vec3.ZERO, camera.target());
        assertEquals(10, distance(camera.position(), camera.target()), 1e-9);
        assertEquals(-10, camera.position().x(), 1e-9);
    }

    @Test void panAndFlyMoveTheCameraAndFocusTogether() {
        SceneCamera camera = new SceneCamera(new Vec3(0, 0, 10), Vec3.ZERO);
        camera.pan(2, 3);
        assertEquals(new Vec3(2, 3, 10), camera.position());
        assertEquals(new Vec3(2, 3, 0), camera.target());
        camera.fly(0, 1, 4);
        assertEquals(new Vec3(2, 4, 6), camera.position());
        assertEquals(new Vec3(2, 4, -4), camera.target());
    }

    @Test void dollyDoesNotCrossTheFocusPoint() {
        SceneCamera camera = new SceneCamera(new Vec3(0, 0, 10), Vec3.ZERO);
        camera.dolly(100);
        assertEquals(.1, distance(camera.position(), camera.target()), 1e-9);
    }

    @Test void rejectsInvalidCameraState() {
        assertThrows(IllegalArgumentException.class, () -> new SceneCamera(Vec3.ZERO, Vec3.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new SceneCamera(new Vec3(Double.NaN, 0, 0), Vec3.ZERO));
    }

    @Test void orthographicViewLooksStraightDownTheZAxisAtTheFocus() {
        SceneCamera camera = new SceneCamera(new Vec3(4, 5, 10), new Vec3(2, 3, 1));
        Vector3f focusInView = camera.orthographicViewMatrix().transformPosition(new Vector3f(2, 3, 1));
        assertEquals(0, focusInView.x, 1e-6);
        assertEquals(0, focusInView.y, 1e-6);
        assertEquals(-camera.focusDistance(), focusInView.z, 1e-6);
    }

    @Test void orthographicPanMovesOnlyTheScenePlane() {
        SceneCamera camera = new SceneCamera(new Vec3(0, 0, 10), Vec3.ZERO);
        camera.panOrthographic(2, -3);
        assertEquals(new Vec3(2, -3, 10), camera.position());
        assertEquals(new Vec3(2, -3, 0), camera.target());
    }

    private static double distance(Vec3 a, Vec3 b) {
        return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2) + Math.pow(a.z() - b.z(), 2));
    }
}
