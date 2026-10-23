package dev.aether;

import dev.aether.audio.AudioSource;
import dev.aether.demo.DemoScenes;
import dev.aether.ecs.World;
import dev.aether.physics.RigidBody;
import dev.aether.render.MeshRenderer;
import dev.aether.scene.Transform;
import dev.aether.script.ScriptComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoScenesTest {
    @Test void showcaseExercisesEveryComponentFamily() {
        World world = new World();
        DemoScenes.populateShowcase(world);
        assertEquals(4, world.entityCount());
        assertEquals(4, world.view(Transform.class).size());
        assertEquals(3, world.view(MeshRenderer.class).size());
        assertEquals(1, world.view(RigidBody.class).size());
        assertFalse(world.view(RigidBody.class).values().iterator().next().useGravity());
        assertEquals(1, world.view(ScriptComponent.class).size());
        assertEquals(1, world.view(AudioSource.class).size());
    }

    @Test void newSceneRestoresCubeShowcase() {
        World world = new World();
        world.create("old scene entity");
        DemoScenes.resetToShowcase(world);
        assertEquals(4, world.entityCount());
        assertTrue(world.entities().stream().map(world::nameOf)
                .anyMatch("Renderer Showcase / PBR Cube"::equals));
        assertEquals(3, world.view(MeshRenderer.class).size());
    }
}
