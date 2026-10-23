package dev.aether.project;

import dev.aether.core.AetherEngine;
import dev.aether.script.Script;
import dev.aether.script.ScriptComponent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ProjectManagerTest {
    @TempDir Path temporary;

    @Test void switchesOnlyAtBoundaryAndRunsOnlyTheNewWorld() throws IOException {
        List<String> updates = new ArrayList<>();
        try (var manager = new ProjectManager(session -> {
            String name = session.project().orElseThrow().config().name();
            assertEquals(AetherEngine.State.NEW, session.engine().state());
            session.world().create(name);
            session.world().addSystem((world, dt) -> updates.add(name));
        })) {
            manager.requestOpen(manifest("First"));
            assertTrue(manager.activeSession().isEmpty());
            assertTrue(manager.applyPending());
            ProjectSession first = manager.activeSession().orElseThrow();
            manager.tick(0);
            manager.requestOpen(manifest("Second"));
            assertSame(first, manager.activeSession().orElseThrow());
            manager.tick(0);
            manager.applyPending();
            ProjectSession second = manager.activeSession().orElseThrow();
            manager.tick(0);
            assertNotSame(first.world(), second.world());
            assertEquals(ProjectSession.State.CLOSED, first.state());
            assertEquals(AetherEngine.State.CLOSED, first.engine().state());
            assertEquals(0, first.world().entityCount());
            assertEquals(List.of("First", "First", "Second"), updates);
            assertEquals("Second", second.world().nameOf(second.world().entities().getFirst()));
        }
    }

    @Test void invalidOpenPreservesPausedSessionAndItsEntities() throws IOException {
        try (var manager = new ProjectManager()) {
            manager.requestOpen(manifest("Good"));
            manager.applyPending();
            ProjectSession original = manager.activeSession().orElseThrow();
            original.world().create("keep");
            original.engine().pause();
            Path invalid = temporary.resolve("Bad.aetherproj");
            Files.writeString(invalid, "{");
            manager.requestOpen(invalid);
            assertThrows(ProjectFormatException.class, manager::applyPending);
            assertSame(original, manager.activeSession().orElseThrow());
            assertEquals(AetherEngine.State.PAUSED, original.engine().state());
            assertEquals(1, original.world().entityCount());
            assertFalse(manager.hasPending());
        }
    }

    @Test void failedActivationClosesPreparedSessionAndRetainsPreviousSession() throws IOException {
        ProjectSession[] candidate = {null};
        try (var manager = new ProjectManager(session -> {
            if (session.project().orElseThrow().config().name().equals("Bad")) {
                candidate[0] = session;
                session.engine().start(); // A host must leave prepared engines in NEW state.
            }
        })) {
            manager.requestOpen(manifest("Good"));
            manager.applyPending();
            ProjectSession previous = manager.activeSession().orElseThrow();
            manager.requestOpen(manifest("Bad"));
            assertThrows(IllegalStateException.class, manager::applyPending);
            assertEquals(ProjectSession.State.CLOSED, candidate[0].state());
            assertSame(previous, manager.activeSession().orElseThrow());
        }
    }

    @Test void failedPreparationRetainsOriginalFailureWhenCleanupAlsoFails() throws IOException {
        try (var manager = new ProjectManager(session -> {
            session.own(() -> { throw new IOException("cleanup failed"); });
            throw new IOException("preparation failed");
        })) {
            manager.requestOpen(manifest("Bad"));
            IOException failure = assertThrows(IOException.class, manager::applyPending);
            assertEquals("preparation failed", failure.getMessage());
            assertEquals(1, failure.getSuppressed().length);
            assertTrue(manager.activeSession().isEmpty());
        }
    }

    @Test void failedPreparationClosesCandidateResourcesAndPreservesCurrentSession() throws IOException {
        List<String> cleanup = new ArrayList<>();
        ProjectSession[] failed = {null};
        try (var manager = new ProjectManager(session -> {
            if (session.project().orElseThrow().config().name().equals("Bad")) {
                failed[0] = session;
                session.own(() -> cleanup.add("first"));
                session.own(() -> cleanup.add("second"));
                throw new IOException("initialization failed");
            }
        })) {
            manager.requestOpen(manifest("Good"));
            manager.applyPending();
            ProjectSession original = manager.activeSession().orElseThrow();
            manager.requestOpen(manifest("Bad"));
            assertThrows(IOException.class, manager::applyPending);
            assertEquals(List.of("second", "first"), cleanup);
            assertEquals(ProjectSession.State.CLOSED, failed[0].state());
            assertSame(original, manager.activeSession().orElseThrow());
            assertEquals(AetherEngine.State.RUNNING, original.engine().state());
        }
    }

    @Test void closesScriptsBeforeOwnedResourcesAndContinuesAfterResourceFailures() throws IOException {
        List<String> order = new ArrayList<>();
        var manager = new ProjectManager(session -> {
            var entity = session.world().create("script");
            session.world().add(entity, new ScriptComponent(new Script() {
                public void onDestroy(dev.aether.ecs.World world, dev.aether.ecs.Entity e) {
                    order.add("script");
                }
            }));
            session.own(() -> order.add("loader"));
            session.own(() -> { order.add("asset"); throw new IOException("cleanup"); });
        });
        manager.requestOpen(manifest("Game"));
        manager.applyPending();
        ProjectSession original = manager.activeSession().orElseThrow();
        manager.tick(0);
        RuntimeException failure = assertThrows(RuntimeException.class, manager::close);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(List.of("script", "asset", "loader"), order);
        assertEquals(ProjectSession.State.CLOSED, original.state());
        assertTrue(manager.activeSession().isEmpty());
        assertDoesNotThrow(manager::close);
    }

    @Test void cleanupFailureAfterActivationKeepsNewSessionActive() throws IOException {
        try (var manager = new ProjectManager(session -> {
            if (session.project().orElseThrow().config().name().equals("Old"))
                session.own(() -> { throw new IOException("old resource failed"); });
        })) {
            manager.requestOpen(manifest("Old"));
            manager.applyPending();
            manager.requestOpen(manifest("New"));
            assertThrows(RuntimeException.class, manager::applyPending);
            assertEquals("New", manager.activeSession().orElseThrow().project().orElseThrow().config().name());
            manager.tick(0);
        }
    }

    @Test void requestsDuringUpdateWaitForTheNextBoundary() throws IOException {
        try (var manager = new ProjectManager()) {
            manager.requestReset(session -> session.world().addSystem((world, dt) -> {
                manager.requestReset(next -> next.world().create("next world"));
                assertThrows(IllegalStateException.class, manager::applyPending);
                assertThrows(IllegalStateException.class, manager::close);
                world.create("deferred entity");
            }));
            manager.applyPending();
            ProjectSession first = manager.activeSession().orElseThrow();
            manager.tick(0);
            assertSame(first, manager.activeSession().orElseThrow());
            assertEquals(1, first.world().entityCount());
            manager.applyPending();
            assertNotSame(first, manager.activeSession().orElseThrow());
            assertEquals("next world", manager.activeSession().orElseThrow().world().nameOf(
                    manager.activeSession().orElseThrow().world().entities().getFirst()));
        }
    }

    @Test void repeatedResetsPreserveProjectButNeverAccumulateSystems() throws IOException {
        int[] updates = {0};
        try (var manager = new ProjectManager()) {
            Path file = manifest("Game");
            manager.requestOpen(file);
            manager.applyPending();
            ProjectFile project = manager.activeSession().orElseThrow().project().orElseThrow();
            for (int index = 0; index < 12; index++) {
                manager.requestReset(session -> session.world().addSystem((world, dt) -> updates[0]++));
                manager.applyPending();
                manager.tick(0);
                assertEquals(Optional.of(project), manager.activeSession().orElseThrow().project());
            }
            assertEquals(12, updates[0]);
        }
    }

    @Test void lastQueuedRequestWinsAndCloseDiscardsQueuedWork() throws IOException {
        int[] initialized = {0};
        var manager = new ProjectManager(session -> initialized[0]++);
        manager.requestOpen(manifest("Ignored"));
        manager.requestOpen(manifest("Selected"));
        manager.applyPending();
        assertEquals(1, initialized[0]);
        assertEquals("Selected", manager.activeSession().orElseThrow().project().orElseThrow().config().name());
        manager.requestOpen(manifest("Never"));
        manager.close();
        assertFalse(manager.hasPending());
        assertEquals(1, initialized[0]);
        assertThrows(IllegalStateException.class, manager::applyPending);
        assertThrows(IllegalStateException.class, () -> manager.requestClose());
        assertThrows(IllegalStateException.class, () -> manager.tick(0));
    }

    @Test void validatesAssetsSceneAndJarBeforePreparingASession() throws IOException {
        try (var manager = new ProjectManager()) {
            Path file = manifest("MissingAssets");
            Files.delete(file.getParent().resolve("assets"));
            manager.requestOpen(file);
            assertThrows(IOException.class, manager::applyPending);
            Path scene = configuredManifest("Scene", Optional.of(Path.of("missing.scene")), Optional.empty());
            manager.requestOpen(scene);
            assertThrows(IOException.class, manager::applyPending);
            Path jar = configuredManifest("Jar", Optional.empty(),
                    Optional.of(new GameModuleConfig(Path.of("missing.jar"), "games.Main")));
            manager.requestOpen(jar);
            assertThrows(IOException.class, manager::applyPending);
            assertTrue(manager.activeSession().isEmpty());
        }
    }

    @Test void defaultHostRejectsConfiguredLoadersInsteadOfSilentlyIgnoringThem() throws IOException {
        Path file = configuredManifest("Scene", Optional.of(Path.of("main.scene")), Optional.empty());
        Files.writeString(file.getParent().resolve("main.scene"), "future scene format");
        try (var manager = new ProjectManager()) {
            manager.requestOpen(file);
            IOException failure = assertThrows(IOException.class, manager::applyPending);
            assertTrue(failure.getMessage().contains("not configured"));
            assertTrue(manager.activeSession().isEmpty());
        }
    }

    @Test void validatesArgumentsAndClosesPreparedOrActiveResourcesOnlyOnce() throws IOException {
        assertThrows(NullPointerException.class, () -> new ProjectManager(null));
        try (var manager = new ProjectManager()) {
            assertThrows(NullPointerException.class, () -> manager.requestOpen(null));
            assertThrows(NullPointerException.class, () -> manager.requestReset(null));
            assertThrows(IllegalArgumentException.class, () -> manager.tick(Double.NaN));
            assertFalse(manager.applyPending());
            manager.requestClose();
            assertFalse(manager.applyPending());
        }
        ProjectSession session = new ProjectSession(Optional.empty());
        int[] closed = {0};
        AutoCloseable resource = () -> closed[0]++;
        session.own(resource);
        assertThrows(IllegalArgumentException.class, () -> session.own(resource));
        session.close();
        session.close();
        assertEquals(1, closed[0]);
        assertThrows(IllegalStateException.class, () -> session.own(() -> { }));
    }

    private Path manifest(String name) throws IOException {
        return configuredManifest(name, Optional.empty(), Optional.empty());
    }

    private Path configuredManifest(String name, Optional<Path> scene, Optional<GameModuleConfig> module)
            throws IOException {
        Path root = Files.createDirectory(temporary.resolve(name));
        Files.createDirectory(root.resolve("assets"));
        Path file = root.resolve(name + ".aetherproj");
        new ProjectSerializer().write(file, new ProjectConfig(1, name, Path.of("assets"), scene, module));
        return file;
    }
}
