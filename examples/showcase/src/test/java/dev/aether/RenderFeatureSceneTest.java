package dev.aether;

import dev.aether.demo.RenderFeatureScene;
import dev.aether.ecs.World;
import dev.aether.render.MeshRenderer;
import dev.aether.render.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderFeatureSceneTest {
    @Test void featureLabIsHeadlessAndDeclaresEveryDemonstrationMaterial() {
        World world = new World();
        world.create("discard me");
        RenderFeatureScene.open(world);

        assertEquals(4, world.entityCount());
        assertEquals(4, world.view(MeshRenderer.class).size());
        assertEquals(4, world.view(Material.class).size());
        assertEquals(1, world.view(Material.class).values().stream().filter(m -> m.emissive() == 6).count());
        assertTrue(world.view(MeshRenderer.class).values().stream().map(MeshRenderer::materialId).toList()
                .containsAll(java.util.List.of("feature/ground", "feature/metal", "feature/emissive", "feature/occluder")));
    }

}
