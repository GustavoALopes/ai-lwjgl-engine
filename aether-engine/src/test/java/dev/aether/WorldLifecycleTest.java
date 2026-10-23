package dev.aether;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.ecs.WorldSystem;
import dev.aether.script.Script;
import dev.aether.script.ScriptComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldLifecycleTest {
    @Test void closesSystemsInReverseOrderOnceAndReleasesWorldState() {
        World world = new World();
        List<Integer> order = new ArrayList<>();
        world.create("entity");
        world.addSystem(system(() -> order.add(1)));
        world.addSystem(system(() -> order.add(2)));
        world.close();
        world.close();
        assertEquals(List.of(2, 1), order);
        assertEquals(0, world.entityCount());
        assertThrows(IllegalStateException.class, () -> world.create("late"));
        assertThrows(IllegalStateException.class, () -> world.addSystem((w, dt) -> { }));
        assertThrows(IllegalStateException.class, () -> world.update(0));
    }

    @Test void cleanupFailuresDoNotPreventOtherSystemsFromClosing() {
        World world = new World();
        List<Integer> order = new ArrayList<>();
        world.addSystem(system(() -> order.add(1)));
        world.addSystem(system(() -> { order.add(2); throw new IllegalArgumentException("failure"); }));
        RuntimeException failure = assertThrows(RuntimeException.class, world::close);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(List.of(2, 1), order);
        assertDoesNotThrow(world::close);
        assertThrows(IllegalStateException.class, () -> world.update(0));
    }

    @Test void engineCannotCloseDuringIterationAndRemainsRunning() {
        try (var engine = Aether.createEngine()) {
            engine.world().addSystem((world, dt) ->
                    assertThrows(IllegalStateException.class, engine::close));
            engine.start();
            engine.tick(0);
            assertEquals(dev.aether.core.AetherEngine.State.RUNNING, engine.state());
        }
    }

    @Test void destroysOnlyInitializedScriptsAndContinuesAfterCallbackFailure() {
        var engine = Aether.createEngine();
        List<String> destroyed = new ArrayList<>();
        addScript(engine.world(), "first", destroyed, false);
        addScript(engine.world(), "second", destroyed, true);
        engine.start();
        engine.tick(0);
        addScript(engine.world(), "never initialized", destroyed, false);
        assertThrows(RuntimeException.class, engine::close);
        assertEquals(List.of("first", "second"), destroyed);
        assertEquals(dev.aether.core.AetherEngine.State.CLOSED, engine.state());
        assertEquals(0, engine.world().entityCount());
        assertDoesNotThrow(engine::close);
    }

    private void addScript(World world, String name, List<String> destroyed, boolean fail) {
        Entity entity = world.create(name);
        world.add(entity, new ScriptComponent(new Script() {
            @Override public void onDestroy(World w, Entity e) {
                destroyed.add(name);
                assertEquals(name, w.nameOf(e), "Script destruction precedes clearing entities");
                if (fail) throw new IllegalStateException("script failure");
            }
        }));
    }

    private WorldSystem system(Runnable cleanup) {
        return new WorldSystem() {
            public void update(World world, double dt) { }
            public void close(World world) { cleanup.run(); }
        };
    }
}
