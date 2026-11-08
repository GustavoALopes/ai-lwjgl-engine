package dev.aether.render;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import java.util.Set;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CutawayVisibilityTest {
    private final Matrix4f perspective = new Matrix4f().perspective((float) Math.toRadians(60), 1, .1f, 100);

    @Test void onlyWallsBetweenActiveViewAndTargetAreCutAway() {
        try (var world = new World()) {
            target(world);
            var front = wall(world, new Vec3(0, 0, 3), new Vec3(4, 4, .2));
            var back = wall(world, new Vec3(0, 0, -3), new Vec3(4, 4, .2));
            assertEquals(Set.of(front), CutawayVisibility.obstructing(world, perspective, view(0, 0, 10)));
            assertEquals(Set.of(back), CutawayVisibility.obstructing(world, perspective, view(0, 0, -10)));
            assertTrue(CutawayVisibility.obstructing(world, perspective, view(0, 0, 2)).isEmpty());
            assertTrue(CutawayVisibility.obstructing(world, perspective, view(0, 12, 1)).isEmpty());
        }
    }

    @Test void cornerViewsCutAwayBothObstructingWallsWithoutChangingWorld() {
        try (var world = new World()) {
            target(world);
            var south = wall(world, new Vec3(0, 0, 3), new Vec3(3.5, 4, .2));
            var east = wall(world, new Vec3(3, 0, 0), new Vec3(.2, 4, 3.5));
            var before = world.view(Transform.class);
            assertEquals(Set.of(south, east), CutawayVisibility.obstructing(world, perspective, view(8, 0, 8)));
            assertEquals(before, world.view(Transform.class));
            assertEquals(3, world.entityCount());
        }
    }

    @Test void orthographicRaysDetectPartialObstructionAndIgnoreWallsBehindTarget() {
        try (var world = new World()) {
            target(world);
            var partial = wall(world, new Vec3(.8, 0, 3), new Vec3(.3, 3, .2));
            wall(world, new Vec3(0, 0, -3), new Vec3(4, 4, .2));
            var ortho = new Matrix4f().ortho(-5, 5, -5, 5, .1f, 100);
            assertEquals(Set.of(partial), CutawayVisibility.obstructing(world, ortho, view(0, 0, 10)));
        }
    }

    @Test void removingTargetRestoresVisibilityAndNewWorldHasIndependentResults() {
        try (var world = new World(); var other = new World()) {
            var target = target(world);
            wall(world, new Vec3(0, 0, 3), new Vec3(4, 4, .2));
            assertFalse(CutawayVisibility.obstructing(world, perspective, view(0, 0, 10)).isEmpty());
            world.destroy(target);
            assertTrue(CutawayVisibility.obstructing(world, perspective, view(0, 0, 10)).isEmpty());
            assertTrue(CutawayVisibility.obstructing(other, perspective, view(0, 0, 10)).isEmpty());
        }
    }

    @Test void rotatedWallsUseTheirOrientedBounds() {
        try (var world = new World()) {
            target(world);
            var wall = wall(world, new Vec3(0, 0, 3), new Vec3(4, 4, .2));
            world.add(wall, new Transform(new Vec3(0, 0, 3), new Vec3(0, 35, 0), new Vec3(4, 4, .2)));
            assertEquals(Set.of(wall), CutawayVisibility.obstructing(world, perspective, view(0, 0, 10)));
        }
    }

    @Test void transitionsGraduallyAndReversesWithoutOvershoot() {
        float progress = CutawayVisibility.approach(0, 1, .05);
        assertEquals(.25, progress, 1e-6);
        progress = CutawayVisibility.approach(progress, 1, .1);
        progress = CutawayVisibility.approach(progress, 1, .1);
        assertEquals(1, progress);
        progress = CutawayVisibility.approach(progress, 0, .05);
        assertTrue(progress > 0 && progress < 1);
        for (int i = 0; i < 10; i++) progress = CutawayVisibility.approach(progress, 0, .05);
        assertEquals(0, progress);
    }

    @Test void roomWallsLowerOnCameraSideAndRestoreWhenCameraMovesInsideOrAroundRoom() {
        try (var world = new World()) {
            var south = wall(world, new Vec3(0, 0, 3), new Vec3(3.5, 4, .2));
            var east = wall(world, new Vec3(3, 0, 0), new Vec3(.2, 4, 3.5));
            world.add(south, new CutawaySurface(.6, new Vec3(0, 0, 1)));
            world.add(east, new CutawaySurface(.6, new Vec3(1, 0, 0)));
            assertEquals(Set.of(south, east), CutawayVisibility.obstructing(world, perspective, view(8, 5, 8)));
            assertEquals(Set.of(south), CutawayVisibility.obstructing(world, perspective, view(-8, 5, 8)));
            assertTrue(CutawayVisibility.obstructing(world, perspective, view(0, 0, 2)).isEmpty());
            var ortho = new Matrix4f().ortho(-10, 10, -10, 10, .1f, 100);
            assertEquals(Set.of(south, east), CutawayVisibility.obstructing(world, ortho, view(8, 5, 8)));
        }
    }

    private Matrix4f view(float x, float y, float z) {
        return new Matrix4f().lookAt(x, y, z, 0, 0, 0, 0, 1, 0);
    }

    private Entity target(World world) {
        var entity = world.create("Target");
        world.add(entity, Transform.identity());
        world.add(entity, new CutawayTarget(.2));
        return entity;
    }

    private Entity wall(World world, Vec3 position, Vec3 scale) {
        var entity = world.create("Wall");
        world.add(entity, new Transform(position, Vec3.ZERO, scale));
        world.add(entity, new MeshRenderer("primitive/cube", "wall"));
        world.add(entity, new CutawaySurface(.12));
        return entity;
    }
}
