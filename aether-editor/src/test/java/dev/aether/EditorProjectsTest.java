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
    @Test void projectCreationWaitsForDiscardAndUpdatesImmutableRecentHistory() {
        Path file = temporary.resolve("created/Game.aetherproj");
        try (var projects = new EditorProjects()) {
            projects.markDirty();
            projects.requestCreate(file, "Created");
            projects.update(0);
            assertFalse(Files.exists(file));
            projects.cancel();
            assertFalse(Files.exists(file));
            projects.requestCreate(file, "Created");
            projects.discardAndContinue();
            projects.update(0);
            assertTrue(Files.exists(file));
            assertEquals(java.util.List.of(file.toAbsolutePath()), projects.recentProjects());
            assertThrows(UnsupportedOperationException.class, () -> projects.recentProjects().clear());
            projects.requestReload();
            projects.update(0);
            assertEquals(1, projects.recentProjects().size());
        }
    }

    @Test void recentHistoryIsBoundedAndFailedOpensAreNotAdded() {
        try (var projects = new EditorProjects()) {
            for (int index = 0; index < 12; index++) {
                projects.requestCreate(temporary.resolve("project-" + index + "/Game.aetherproj"), "Game " + index);
                projects.update(0);
            }
            assertEquals(10, projects.recentProjects().size());
            assertEquals(temporary.resolve("project-11/Game.aetherproj"), projects.recentProjects().getFirst());
            projects.requestOpen(temporary.resolve("Missing.aetherproj"));
            projects.update(0);
            assertEquals(10, projects.recentProjects().size());
            assertEquals(temporary.resolve("project-11/Game.aetherproj"), projects.recentProjects().getFirst());
        }
    }
    @TempDir Path temporary;

    @Test void sceneSwitchesAreDeferredAndOldSystemsStop() {
        try (var projects = new EditorProjects()) {
            ProjectSession original = projects.activeSession().orElseThrow();
            int[] updates = {0};
            original.world().addSystem((world, dt) -> updates[0]++);
            projects.update(0);
            projects.requestNewScene();
            assertSame(original, projects.activeSession().orElseThrow());
            projects.update(0);
            assertEquals(ProjectSession.State.CLOSED, original.state());
            assertEquals(1, updates[0]);
            assertEquals(0, projects.activeSession().orElseThrow().world().entityCount());
        }
    }

    @Test void dirtySceneRequiresExplicitDiscardAndCancellationPreservesEdits() {
        try (var projects = new EditorProjects()) {
            ProjectSession original = projects.activeSession().orElseThrow();
            original.world().create("authored entity");
            projects.markDirty();
            projects.requestNewScene();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertTrue(projects.needsConfirmation());
            projects.cancel();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertEquals(1, original.world().entityCount());
            assertTrue(projects.dirty());
            projects.requestNewScene();
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
            projects.requestNewScene();
            projects.markDirty();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertTrue(projects.needsConfirmation());
        }
    }

    @Test void failedOpenKeepsDirtyWorld() {
        try (var projects = new EditorProjects()) {
            ProjectSession original = projects.activeSession().orElseThrow();
            original.world().create("keep me");
            projects.markDirty();
            projects.requestOpen(temporary.resolve("Missing.aetherproj"));
            projects.discardAndContinue();
            projects.update(0);
            assertSame(original, projects.activeSession().orElseThrow());
            assertEquals(1, original.world().entityCount());
            assertTrue(projects.dirty());
            assertTrue(projects.error().isPresent());
        }
    }

    @Test void opensReloadsAndResetsWithinTheProjectContext() throws IOException {
        Path manifest = manifest();
        try (var projects = new EditorProjects()) {
            projects.requestOpen(manifest);
            projects.update(0);
            var original = projects.activeSession().orElseThrow();
            var project = original.project().orElseThrow();
            assertEquals("Test", project.config().name());
            assertEquals(0, original.world().entityCount());
            projects.requestNewScene();
            projects.update(0);
            assertEquals(project, projects.activeSession().orElseThrow().project().orElseThrow());
            projects.requestReload();
            projects.update(0);
            assertNotSame(original, projects.activeSession().orElseThrow());
            projects.requestClose();
            projects.update(0);
            assertTrue(projects.activeSession().isEmpty());
            projects.requestNewScene();
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

    @Test void cleanupFailureReportsErrorButPublishesTheNewSession() {
        try (var projects = new EditorProjects()) {
            var previous = projects.activeSession().orElseThrow();
            previous.own(() -> { throw new IOException("cleanup"); });
            projects.requestNewScene();
            projects.update(0);
            assertNotSame(previous, projects.activeSession().orElseThrow());
            assertEquals(0, projects.activeSession().orElseThrow().world().entityCount());
            assertTrue(projects.error().isPresent());
        }
    }

    @Test void gameSystemFailurePausesSimulationAndKeepsEditorUsable() {
        try (var projects = new EditorProjects()) {
            var session = projects.activeSession().orElseThrow();
            session.world().addSystem((world, dt) -> { throw new IllegalArgumentException("bad game"); });
            projects.update(0);
            assertEquals(AetherEngine.State.PAUSED, session.engine().state());
            assertTrue(projects.error().orElseThrow().contains("bad game"));
            projects.requestNewScene();
            projects.update(0);
            assertEquals(AetherEngine.State.RUNNING, projects.activeSession().orElseThrow().engine().state());
        }
    }

    @Test void invalidTimeDoesNotApplyActionsAndClosedControllerRejectsWork() {
        var projects = new EditorProjects();
        ProjectSession original = projects.activeSession().orElseThrow();
        projects.requestNewScene();
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
