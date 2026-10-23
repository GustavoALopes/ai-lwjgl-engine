package dev.aether;

import dev.aether.core.AetherEngine;
import dev.aether.editor.EditorProjects;
import dev.aether.project.ProjectConfig;
import dev.aether.project.ProjectSerializer;
import dev.aether.project.ProjectSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EditorProjectsTest {
    @TempDir Path temporary;

    @Test void demoSwitchesAreDeferredAndOldTetrisSystemsStop() {
        try (var projects = new EditorProjects()) {
            ProjectSession showcase = projects.activeSession().orElseThrow();
            projects.requestScene(EditorProjects.Scene.TETRIS);
            assertSame(showcase, projects.activeSession().orElseThrow());
            projects.update(0);
            ProjectSession tetrisSession = projects.activeSession().orElseThrow();
            var game = projects.tetris().orElseThrow().game();
            projects.update(.1);
            double previousElapsed = game.snapshot().elapsedSeconds();
            projects.requestScene(EditorProjects.Scene.SHOWCASE);
            projects.update(.1);
            assertEquals(ProjectSession.State.CLOSED, tetrisSession.state());
            assertEquals(previousElapsed, game.snapshot().elapsedSeconds());
            assertEquals(4, projects.activeSession().orElseThrow().world().entityCount());
            assertTrue(projects.tetris().isEmpty());
        }
    }

    @Test void dirtySceneRequiresExplicitDiscardAndCancellationPreservesEdits() {
        try (var projects = new EditorProjects()) {
            ProjectSession original = projects.activeSession().orElseThrow();
            projects.markDirty();
            projects.requestScene(EditorProjects.Scene.EMPTY);
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertTrue(projects.needsConfirmation());
            projects.cancel();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertTrue(projects.dirty());
            projects.requestScene(EditorProjects.Scene.EMPTY);
            projects.discardAndContinue();
            projects.update(0);
            assertNotSame(original, projects.activeSession().orElseThrow());
            assertFalse(projects.dirty());
            assertEquals(0, projects.activeSession().orElseThrow().world().entityCount());
        }
    }

    @Test void editsMadeAfterQueuingAnActionAlsoRequireConfirmation() {
        try (var projects = new EditorProjects()) {
            ProjectSession original = projects.activeSession().orElseThrow();
            projects.requestScene(EditorProjects.Scene.EMPTY);
            projects.markDirty();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertTrue(projects.needsConfirmation());
        }
    }

    @Test void failedOpenKeepsDirtySceneAndGamePresentationState() {
        try (var projects = new EditorProjects()) {
            projects.requestScene(EditorProjects.Scene.TETRIS);
            projects.update(0);
            ProjectSession original = projects.activeSession().orElseThrow();
            var tetris = projects.tetris().orElseThrow();
            projects.markDirty();
            projects.requestOpen(temporary.resolve("Missing.aetherproj"));
            projects.discardAndContinue();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertSame(tetris, projects.tetris().orElseThrow());
            assertTrue(projects.dirty());
            assertTrue(projects.error().isPresent());
        }
    }

    @Test void opensMinimalProjectAndResetsScenesWithinItsProjectContext() throws IOException {
        Path manifest = manifest();
        try (var projects = new EditorProjects()) {
            projects.requestOpen(manifest);
            projects.update(0);
            var project = projects.activeSession().orElseThrow().project().orElseThrow();
            assertEquals("Test", project.config().name());
            assertEquals(EditorProjects.Scene.EMPTY, projects.scene());
            assertEquals(0, projects.activeSession().orElseThrow().world().entityCount());
            projects.requestScene(EditorProjects.Scene.TETRIS);
            projects.update(0);
            assertEquals(project, projects.activeSession().orElseThrow().project().orElseThrow());
            assertEquals(201, projects.activeSession().orElseThrow().world().entityCount());
            projects.requestClose();
            projects.update(0);
            assertTrue(projects.activeSession().isEmpty());
            assertTrue(projects.tetris().isEmpty());
            projects.requestScene(EditorProjects.Scene.EMPTY);
            projects.update(0);
            assertTrue(projects.activeSession().orElseThrow().project().isEmpty());
        }
    }

    @Test void quitHonorsUnsavedChangesAndLeavesCleanupToHost() {
        var projects = new EditorProjects();
        ProjectSession original = projects.activeSession().orElseThrow();
        projects.markDirty();
        projects.requestQuit();
        projects.update(0);
        assertFalse(projects.quitRequested());
        projects.cancel();
        projects.update(0);
        assertFalse(projects.quitRequested());
        projects.requestQuit();
        projects.discardAndContinue();
        projects.update(0);
        assertTrue(projects.quitRequested());
        assertEquals(ProjectSession.State.ACTIVE, original.state());
        projects.close();
        assertEquals(ProjectSession.State.CLOSED, original.state());
        assertDoesNotThrow(projects::close);
    }

    @Test void pausedSessionStaysPausedOnFailedOpenAndNewSessionStartsRunning() throws IOException {
        try (var projects = new EditorProjects()) {
            var original = projects.activeSession().orElseThrow();
            original.engine().pause();
            projects.requestOpen(temporary.resolve("missing.aetherproj"));
            projects.update(1);
            assertEquals(AetherEngine.State.PAUSED, original.engine().state());
            projects.requestOpen(manifest());
            projects.update(0);
            assertEquals(AetherEngine.State.CLOSED, original.engine().state());
            assertEquals(AetherEngine.State.RUNNING, projects.activeSession().orElseThrow().engine().state());
            assertTrue(projects.error().isEmpty());
        }
    }

    @Test void cleanupFailureReportsErrorButUpdatesPresentationToNewSession() {
        try (var projects = new EditorProjects()) {
            projects.requestScene(EditorProjects.Scene.TETRIS);
            projects.update(0);
            projects.activeSession().orElseThrow().own(() -> { throw new IOException("cleanup"); });
            projects.requestScene(EditorProjects.Scene.EMPTY);
            projects.update(0);
            assertEquals(EditorProjects.Scene.EMPTY, projects.scene());
            assertTrue(projects.tetris().isEmpty());
            assertEquals(0, projects.activeSession().orElseThrow().world().entityCount());
            assertTrue(projects.error().isPresent());
        }
    }

    @Test void invalidTimeDoesNotApplyQueuedActionsAndClosedControllerRejectsWork() {
        var projects = new EditorProjects();
        ProjectSession original = projects.activeSession().orElseThrow();
        projects.requestScene(EditorProjects.Scene.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> projects.update(-1));
        assertSame(original, projects.activeSession().orElseThrow());
        projects.close();
        assertThrows(IllegalStateException.class, projects::markDirty);
        assertThrows(IllegalStateException.class, projects::requestClose);
        assertThrows(IllegalStateException.class, () -> projects.update(0));
    }

    @Test void closingDuringUpdateIsRejectedWithoutLosingOwnership() {
        try (var projects = new EditorProjects()) {
            ProjectSession original = projects.activeSession().orElseThrow();
            original.world().addSystem((world, dt) ->
                    assertThrows(IllegalStateException.class, projects::close));
            projects.update(0);
            assertEquals(ProjectSession.State.ACTIVE, original.state());
        }
    }

    private Path manifest() throws IOException {
        Files.createDirectories(temporary.resolve("assets"));
        Path file = temporary.resolve("Test.aetherproj");
        new ProjectSerializer().write(file, ProjectConfig.create("Test", Path.of("assets")));
        return file;
    }
}
