package dev.aether.render;

import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraMatricesTest {
    @Test void selectsFirstActiveCameraInEntityOrderAndFallsBackWhenRemoved() {
        try (var world = new World()) {
            assertTrue(CameraMatrices.active(world, 1).isEmpty());
            var first = world.create("First");
            var second = world.create("Second");
            world.add(second, Camera.perspective(55, .1, 100));
            world.add(second, Transform.identity());
            world.add(first, Camera.perspective(55, .1, 100));
            assertEquals(second, CameraMatrices.active(world, 1).orElseThrow().entity());
            world.add(first, Transform.identity());
            assertEquals(first, CameraMatrices.active(world, 1).orElseThrow().entity());
            world.add(first, new Camera(Camera.Projection.PERSPECTIVE, 55, 10, .1, 100, false));
            assertEquals(second, CameraMatrices.active(world, 1).orElseThrow().entity());
            world.destroy(second);
            assertTrue(CameraMatrices.active(world, 1).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> CameraMatrices.active(world, 0));
        }
    }

    @Test void viewUsesCameraPoseAndIgnoresScaleAndFramesAreSnapshots() {
        try (var world = new World()) {
            var entity = world.create("Camera");
            world.add(entity, Camera.perspective(60, .1, 100));
            world.add(entity, new Transform(new Vec3(0, 8, 14),
                    new Vec3(-Math.toDegrees(Math.atan2(8, 14)), 0, 0), new Vec3(8, 3, 2)));
            var frame = CameraMatrices.active(world, 2).orElseThrow();
            Vector3f target = frame.view().transformPosition(new Vector3f());
            assertEquals(0, target.x, 1e-5);
            assertEquals(0, target.y, 1e-5);
            assertEquals(-Math.hypot(8, 14), target.z, 1e-5);
            assertEquals(frame.projection().m11() / 2, frame.projection().m00(), 1e-6);
            frame.view().identity();
            frame.projection().identity();
            assertEquals(target, frame.view().transformPosition(new Vector3f()));
            assertNotEquals(1, frame.projection().m11());
        }
    }

    @Test void orthographicProjectionUsesDeclaredHeight() {
        try (var world = new World()) {
            var entity = world.create("Camera");
            world.add(entity, Transform.identity());
            world.add(entity, new Camera(Camera.Projection.ORTHOGRAPHIC, 55, 10, .1, 100, true));
            var projection = CameraMatrices.active(world, 2).orElseThrow().projection();
            assertEquals(.2, projection.m11(), 1e-6);
            assertEquals(.1, projection.m00(), 1e-6);
        }
    }
}
