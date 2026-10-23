package dev.aether;

import dev.aether.core.AetherEngine;
import dev.aether.ecs.World;
import dev.aether.physics.PhysicsSystem;
import dev.aether.script.ScriptSystem;

public final class Aether {
    private Aether() {
    }

    public static AetherEngine createEngine() {
        World w = new World();
        w.addSystem(new ScriptSystem());
        w.addSystem(new PhysicsSystem());
        return new AetherEngine(w);
    }
}
