package dev.aether.demo;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.render.MeshRenderer;
import dev.aether.scene.Transform;
import java.util.List;
import java.util.Objects;

/**
 * Headless scene description for inspecting the renderer's HDR, SSAO, soft-shadow,
 * and image-based-lighting passes.  Materials are intentionally declarative: the
 * OpenGL adapter interprets them, while ECS tests need no graphics context.
 */
public final class RenderFeatureScene {
    private RenderFeatureScene() { }

    /** Replaces visible entities with a composition designed to show every render feature. */
    public static List<Entity> open(World world) {
        Objects.requireNonNull(world, "world");
        world.entities().forEach(world::destroy);

        Entity ground = add(world, "Feature Lab / Shadow Receiver", new Vec3(0, -1.15, 0),
                Vec3.ZERO, new Vec3(7, .15, 7), "feature/ground");
        Entity metal = add(world, "Feature Lab / IBL Metal", new Vec3(-1.55, .15, .1),
                new Vec3(0, 32, 0), new Vec3(.95, .95, .95), "feature/metal");
        Entity warm = add(world, "Feature Lab / HDR Emitter", new Vec3(1.45, .05, -.25),
                new Vec3(0, -22, 0), new Vec3(.8, .8, .8), "feature/emissive");
        Entity contact = add(world, "Feature Lab / SSAO Contact", new Vec3(.05, -.63, 1.2),
                new Vec3(0, 18, 0), new Vec3(1.55, .22, 1.55), "feature/occluder");
        return List.of(ground, metal, warm, contact);
    }

    private static Entity add(World world, String name, Vec3 position, Vec3 rotation, Vec3 scale, String material) {
        Entity entity = world.create(name);
        world.add(entity, new Transform(position, rotation, scale));
        world.add(entity, new MeshRenderer("primitive/cube", material));
        return entity;
    }
}
