package dev.aether;

import dev.aether.core.AetherEngine;
import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.physics.PhysicsSystem;
import dev.aether.physics.RigidBody;
import dev.aether.scene.Transform;
import dev.aether.script.Script;
import dev.aether.script.ScriptComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AetherEngineTest {
    @Test void physicsMovesGravityBody() {
        World world = new World();
        world.addSystem(new PhysicsSystem());
        Entity entity = world.create("falling");
        world.add(entity, Transform.identity());
        world.add(entity, new RigidBody(1, Vec3.ZERO, true));
        world.update(1);
        assertEquals(-9.81, world.get(entity, Transform.class).orElseThrow().position().y(), 1e-9);
    }

    @Test void structuralChangesAreDeferred() {
        World world = new World();
        Entity entity = world.create("source");
        world.addSystem((w, dt) -> w.destroy(entity));
        world.update(0);
        assertFalse(world.exists(entity));
    }

    @Test void entityCreatedDuringUpdateCanReceiveComponents() {
        World world = new World();
        world.addSystem((w, dt) -> {
            Entity entity = w.create("spawned");
            w.add(entity, Transform.identity());
        });
        world.update(0);
        assertEquals(1, world.entityCount());
        assertEquals(1, world.view(Transform.class).size());
    }

    @Test void scriptLifecycleAndEngineState() {
        try (var engine = Aether.createEngine()) {
            Entity entity = engine.world().create("scripted");
            int[] calls = {0};
            engine.world().add(entity, new ScriptComponent(new Script() {
                public void onCreate(World world, Entity e) { calls[0]++; }
                public void onUpdate(World world, Entity e, double dt) { calls[0]++; }
            }));
            assertThrows(IllegalStateException.class, () -> engine.tick(0));
            engine.start();
            engine.tick(.016);
            assertEquals(2, calls[0]);
        }
    }

    @Test void pausedEngineDoesNotTickAndCanResume() {
        try (var engine = Aether.createEngine()) {
            int[] calls = {0};
            engine.world().addSystem((world, dt) -> calls[0]++);
            engine.start();
            engine.tick(0);
            engine.pause();
            assertEquals(AetherEngine.State.PAUSED, engine.state());
            assertThrows(IllegalStateException.class, () -> engine.tick(0));
            engine.resume();
            engine.tick(0);
            assertEquals(2, calls[0]);
        }
    }

    @Test void invalidTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new World().update(-.1));
    }

    @Test void transformScalePreservesPosition() {
        Transform initial = Transform.identity().withPosition(new Vec3(3, 2, 1));
        Transform scaled = initial.withScale(new Vec3(2, 3, 4));
        assertEquals(new Vec3(3, 2, 1), scaled.position());
        assertEquals(new Vec3(2, 3, 4), scaled.scale());
    }
}
