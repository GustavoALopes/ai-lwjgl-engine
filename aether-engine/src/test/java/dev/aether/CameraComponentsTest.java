package dev.aether;

import dev.aether.render.Camera;
import dev.aether.render.CutawaySurface;
import dev.aether.render.CutawayTarget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraComponentsTest {
    @Test void declaresCameraWithoutNativeDependencies() {
        var camera = Camera.perspective(55, .1, 100);
        assertEquals(Camera.Projection.PERSPECTIVE, camera.projection());
        assertTrue(camera.active());
        assertEquals(.1, camera.nearClip());
        assertEquals(100, camera.farClip());
    }

    @Test void rejectsInvalidOpticsAndCutawayValues() {
        for (double fov : new double[] {0, 180, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> Camera.perspective(fov, .1, 100));
        assertThrows(NullPointerException.class, () -> new Camera(null, 55, 10, .1, 100, true));
        assertThrows(IllegalArgumentException.class, () -> Camera.perspective(55, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> Camera.perspective(55, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> new Camera(Camera.Projection.ORTHOGRAPHIC, 55, -1, .1, 100, true));
        for (double value : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new CutawayTarget(value));
            assertThrows(IllegalArgumentException.class, () -> new CutawaySurface(value));
        }
        assertEquals(0, new CutawaySurface(0).remainingHeight());
        assertEquals(6, new CutawaySurface(6).remainingHeight());
        assertThrows(NullPointerException.class, () -> new CutawaySurface(.6, null));
        assertThrows(IllegalArgumentException.class, () -> new CutawaySurface(.6, new dev.aether.math.Vec3(Double.NaN, 0, 0)));
    }
}
