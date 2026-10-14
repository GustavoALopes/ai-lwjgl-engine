package dev.aether.demo;

import dev.aether.audio.*;
import dev.aether.ecs.*;
import dev.aether.math.Vec3;
import dev.aether.physics.RigidBody;
import dev.aether.render.MeshRenderer;
import dev.aether.scene.Transform;
import dev.aether.script.Script;
import dev.aether.script.ScriptComponent;
import java.nio.file.Path;
import java.util.List;

/** Canonical scene for CI and the editor: it deliberately covers each public component family. */
public final class DemoScenes {
    private DemoScenes() { }
    public static List<Entity> populateShowcase(World world) {
        Entity floor = world.create("Physics Playground / Ground");
        world.add(floor, new Transform(new Vec3(0, -2, 0), Vec3.ZERO, new Vec3(12, 1, 12)));
        world.add(floor, new MeshRenderer("primitive/cube", "pbr/checker"));
        Entity cube = world.create("Renderer Showcase / PBR Cube");
        world.add(cube, new Transform(new Vec3(-2, 2, 0), Vec3.ZERO, new Vec3(1, 1, 1)));
        world.add(cube, new MeshRenderer("primitive/cube", "pbr/metallic-blue"));
        // Collision is intentionally not introduced until its full broadphase/narrowphase contract exists;
        // keep the showcase body static so the default editor scene remains inspectable indefinitely.
        world.add(cube, new RigidBody(1, Vec3.ZERO, false));
        Entity spinner = world.create("Scripting Sandbox / Spinner");
        world.add(spinner, new Transform(new Vec3(2, 0, 0), Vec3.ZERO, new Vec3(1, 1, 1)));
        world.add(spinner, new MeshRenderer("primitive/sphere", "pbr/emissive-orange"));
        world.add(spinner, new ScriptComponent(new SpinScript()));
        Entity audio = world.create("Audio / Ambient Emitter");
        world.add(audio, new Transform(new Vec3(0, 1, -3), Vec3.ZERO, new Vec3(1, 1, 1)));
        world.add(audio, new AudioSource(new AudioClip(Path.of("assets/audio/ambient.ogg")), true, 0.35));
        return List.of(floor, cube, spinner, audio);
    }
    private static final class SpinScript implements Script {
        @Override public void onUpdate(World world, Entity entity, double dt) {
            world.get(entity, Transform.class).ifPresent(transform -> world.add(entity, new Transform(transform.position(), transform.rotationDegrees().add(new Vec3(0, 90 * dt, 0)), transform.scale())));
        }
    }
}
