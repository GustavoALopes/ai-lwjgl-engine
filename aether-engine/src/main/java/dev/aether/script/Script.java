package dev.aether.script;

import dev.aether.ecs.*;

public interface Script {
    default void onCreate(World w, Entity e) {
    }

    default void onUpdate(World w, Entity e, double dt) {
    }

    default void onDestroy(World w, Entity e) {
    }
}
