package dev.aether;

import dev.aether.ecs.World;
import dev.aether.editor.SceneDropPlacement;
import dev.aether.math.Vec3;
import dev.aether.render.MeshRenderer;
import dev.aether.render.ModelMesh;
import dev.aether.scene.Transform;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneDropPlacementTest {
    private final Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(55), 1, .1f, 100);
    private final Matrix4f view = new Matrix4f().lookAt(0, 8, 14, 0, 0, 0, 0, 1, 0);

    @Test void hitsFloorTopAndUsesGroundWhenSceneIsEmpty() {
        try (var world = new World()) {
            Vec3 empty = SceneDropPlacement.position(world, projection, view, 0, 0, false);
            assertEquals(0, empty.y(), 1e-4);
            assertEquals(0, empty.z(), 1e-3);
            var floor = world.create("floor");
            world.add(floor, new MeshRenderer("primitive/cube", "floor"));
            world.add(floor, new Transform(new Vec3(0, -2, 0), Vec3.ZERO, new Vec3(8, .5, 8)));
            Vec3 hit = SceneDropPlacement.position(world, projection, view, 0, 0, false);
            assertEquals(-1.5, hit.y(), 1e-4);
        }
    }

    @Test void hitsImportedTrianglesBeforeGroundAndHandlesOrthographicFallback() {
        try (var world = new World()) {
            var mesh = world.create("model");
            world.add(mesh, new MeshRenderer("model.obj", "imported"));
            world.add(mesh, Transform.identity());
            world.add(mesh, new ModelMesh(List.of(new ModelMesh.Part(new float[] {
                    -10, 2, -10, 0, 0, 0, 1, 0,
                    10, 2, -10, 0, 0, 0, 1, 0,
                    0, 2, 10, 0, 0, 0, 1, 0 }, new Vec3(1, 1, 1)))));
            assertEquals(2, SceneDropPlacement.position(world, projection, view, 0, 0, false).y(), 1e-4);
            world.destroy(mesh);
            var ortho = new Matrix4f().ortho(-5, 5, -5, 5, .1f, 100);
            var front = new Matrix4f().lookAt(0, 0, 10, 0, 0, 0, 0, 1, 0);
            Vec3 point = SceneDropPlacement.position(world, ortho, front, .5f, .5f, true);
            assertEquals(2.5, point.x(), 1e-5);
            assertEquals(2.5, point.y(), 1e-5);
            assertEquals(0, point.z(), 1e-5);
        }
    }
}
