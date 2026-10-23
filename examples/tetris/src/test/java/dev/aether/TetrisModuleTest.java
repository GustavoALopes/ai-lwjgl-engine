package dev.aether;

import dev.aether.demo.TetrisModule;
import dev.aether.ecs.World;
import dev.aether.game.*;
import dev.aether.render.Material;
import dev.aether.render.MeshRenderer;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TetrisModuleTest {
    @Test void initializesItsOwnSceneAndSuppliesGameUiAndMaterialData() {
        try (var module = new TetrisModule(); var world = new World()) {
            module.initialize(context(world));
            assertEquals(201, world.entityCount());
            assertEquals(200, world.view(Material.class).size());
            assertEquals(GamePresentation.ViewMode.TWO_D, module.presentation().viewMode());
            assertTrue(module.presentation().labels().stream().anyMatch(label -> label.text().equals("SCORE")));
            assertTrue(module.presentation().controls().contains("Space"));
            world.update(2);
            assertTrue(module.presentation().labels().stream().anyMatch(label -> label.text().equals("00:02")));
        }
    }

    @Test void ownsInputSemanticsAndResetsWithoutRegisteringDuplicateSystems() {
        try (var module = new TetrisModule(); var world = new World()) {
            module.initialize(context(world));
            module.input(new GameInput(Set.of(GameKey.SPACE), Set.of()));
            world.update(0);
            assertEquals(4, occupied(world));
            module.input(new GameInput(Set.of(GameKey.SPACE), Set.of(GameKey.SPACE)));
            world.update(0);
            assertEquals(8, occupied(world));
            for (int index = 0; index < 3; index++) {
                module.input(new GameInput(Set.of(GameKey.R), Set.of(GameKey.R)));
                world.update(0);
                assertEquals(201, world.entityCount());
                assertEquals(4, occupied(world));
            }
            world.update(1);
            assertTrue(module.presentation().labels().stream().anyMatch(label -> label.text().equals("00:01")));
        }
    }

    @Test void validatesLifecycleBeforeAndAfterInitialization() {
        var module = new TetrisModule();
        assertThrows(IllegalStateException.class, module::presentation);
        assertThrows(IllegalStateException.class, () -> module.input(GameInput.empty()));
        try (var world = new World()) {
            module.initialize(context(world));
            assertThrows(IllegalStateException.class, () -> module.initialize(context(world)));
            assertThrows(NullPointerException.class, () -> module.input(null));
            module.close();
            module.close();
            assertThrows(IllegalStateException.class, module::presentation);
            assertThrows(IllegalStateException.class, () -> module.initialize(context(world)));
        }
    }

    private long occupied(World world) {
        return world.view(MeshRenderer.class).values().stream()
                .filter(mesh -> !mesh.materialId().equals("tetris/empty")
                        && !mesh.materialId().equals("tetris/particle")).count();
    }

    private GameContext context(World world) {
        return new GameContext() {
            public World world() { return world; }
            public Path assetDirectory() { return Path.of("assets").toAbsolutePath(); }
            public <T extends AutoCloseable> T own(T resource) { return resource; }
        };
    }
}
