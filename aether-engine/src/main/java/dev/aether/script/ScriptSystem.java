package dev.aether.script;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.ecs.WorldSystem;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptSystem implements WorldSystem {
    private final Map<Entity, Script> initialized = new LinkedHashMap<>();

    @Override
    public void update(World world, double deltaSeconds) {
        world.view(ScriptComponent.class).forEach((entity, component) -> {
            if (!initialized.containsKey(entity)) {
                component.script().onCreate(world, entity);
                initialized.put(entity, component.script());
            }
            component.script().onUpdate(world, entity, deltaSeconds);
        });
    }

    @Override
    public void close(World world) {
        RuntimeException failure = null;
        var scripts = new LinkedHashMap<>(initialized);
        initialized.clear();
        for (var entry : scripts.entrySet()) {
            try {
                entry.getValue().onDestroy(world, entry.getKey());
            } catch (RuntimeException exception) {
                if (failure == null)
                    failure = new IllegalStateException("Failed to destroy scripts");
                failure.addSuppressed(exception);
            }
        }
        if (failure != null)
            throw failure;
    }
}
