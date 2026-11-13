package dev.aether;

import dev.aether.editor.EditorProjects;
import dev.aether.math.Vec3;
import dev.aether.render.MeshRenderer;
import dev.aether.render.ModelMesh;
import dev.aether.scene.Transform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ModelImportTest {
    @TempDir Path directory;

    @Test void importsEachDropAsAnEditableEntityAndMarksSceneDirty() throws Exception {
        try (var projects = new EditorProjects()) {
            projects.requestCreate(directory.resolve("game.aetherproj"), "Test");
            projects.update(0);
            Files.createDirectories(directory.resolve("assets/models"));
            Files.writeString(directory.resolve("assets/models/table.obj"), "v 0 -2 0\nv 1 -2 0\nv 0 1 0\nf 1 2 3\n");
            var first = projects.importObj(Path.of("models/table.obj"), new Vec3(3, -1, 4));
            var world = projects.activeSession().orElseThrow().world();
            assertEquals("table", world.nameOf(first));
            assertEquals(new Vec3(3, 1, 4), world.get(first, Transform.class).orElseThrow().position());
            assertEquals("models/table.obj", world.get(first, MeshRenderer.class).orElseThrow().meshId());
            assertTrue(world.get(first, ModelMesh.class).isPresent());
            assertTrue(projects.dirty());
            var second = projects.importObj(Path.of("models/table.obj"), Vec3.ZERO);
            assertNotEquals(first, second);
            assertEquals("table (1)", world.nameOf(second));
            assertEquals(2, world.entityCount());
            world.destroy(first);
            assertTrue(world.exists(second));
        }
    }

    @Test void failedImportDoesNotMutateSceneOrDirtyStateAndRejectsEscapingPaths() throws Exception {
        try (var projects = new EditorProjects()) {
            assertThrows(IllegalStateException.class, () -> projects.importObj(Path.of("x.obj"), Vec3.ZERO));
            projects.requestCreate(directory.resolve("game.aetherproj"), "Test");
            projects.update(0);
            Files.writeString(directory.resolve("assets/broken.obj"), "broken");
            assertThrows(IOException.class, () -> projects.importObj(Path.of("broken.obj"), Vec3.ZERO));
            assertThrows(IllegalArgumentException.class, () -> projects.importObj(Path.of("../outside.obj"), Vec3.ZERO));
            assertThrows(IllegalArgumentException.class, () -> projects.importObj(directory.resolve("outside.obj"), Vec3.ZERO));
            assertThrows(IllegalArgumentException.class, () -> projects.importObj(Path.of("broken.obj"), new Vec3(Double.NaN, 0, 0)));
            assertEquals(0, projects.activeSession().orElseThrow().world().entityCount());
            assertFalse(projects.dirty());
        }
    }

    @Test void rejectsImportsDuringWorldIterationAndAfterControllerClose() {
        var projects = new EditorProjects();
        projects.requestCreate(directory.resolve("game.aetherproj"), "Test");
        projects.update(0);
        var session = projects.activeSession().orElseThrow();
        session.world().addSystem((world, dt) -> assertThrows(IllegalStateException.class,
                () -> projects.importObj(Path.of("missing.obj"), Vec3.ZERO)));
        session.engine().resume();
        projects.update(.1);
        assertFalse(projects.dirty());
        projects.close();
        assertThrows(IllegalStateException.class, () -> projects.importObj(Path.of("missing.obj"), Vec3.ZERO));
    }
}
