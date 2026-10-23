package dev.aether.project;

import dev.aether.Aether;
import dev.aether.core.AetherEngine;
import dev.aether.ecs.World;
import dev.aether.game.GameContext;
import dev.aether.game.GameModule;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Owns one isolated engine/world and the resources acquired for that session. */
public final class ProjectSession implements AutoCloseable, GameContext {
    public enum State { PREPARED, ACTIVE, CLOSED }

    private final Optional<ProjectFile> project;
    private final AetherEngine engine = Aether.createEngine();
    private final List<AutoCloseable> resources = new ArrayList<>();
    private State state = State.PREPARED;
    private GameModule module;

    ProjectSession(Optional<ProjectFile> project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    public Optional<ProjectFile> project() { return project; }
    public AetherEngine engine() { return engine; }
    public World world() { return engine.world(); }
    public State state() { return state; }
    public Optional<GameModule> module() { return Optional.ofNullable(module); }
    @Override public Path assetDirectory() {
        return project.orElseThrow(() -> new IllegalStateException("Untitled session has no asset directory"))
                .assetDirectory();
    }

    void installModule(GameModule module) {
        if (state != State.PREPARED || this.module != null)
            throw new IllegalStateException("A module can only be installed once in a prepared session");
        this.module = Objects.requireNonNull(module, "module");
    }

    /** Ownership transfers on success. Resources close in reverse acquisition order. */
    public <T extends AutoCloseable> T own(T resource) {
        Objects.requireNonNull(resource, "resource");
        if (state == State.CLOSED)
            throw new IllegalStateException("Session is closed");
        if (resource == module || resources.stream().anyMatch(existing -> existing == resource))
            throw new IllegalArgumentException("Resource is already owned by this session");
        resources.add(resource);
        return resource;
    }

    void activate() {
        if (state != State.PREPARED)
            throw new IllegalStateException("Expected a prepared session");
        engine.start();
        state = State.ACTIVE;
    }

    @Override
    public void close() {
        if (state == State.CLOSED)
            return;
        if (world().isUpdating())
            throw new IllegalStateException("Cannot close a session during update");
        state = State.CLOSED;
        RuntimeException failure = null;
        try {
            engine.close();
        } catch (RuntimeException exception) {
            failure = new IllegalStateException("Failed to close project session");
            failure.addSuppressed(exception);
        }
        if (module != null) {
            try {
                module.close();
            } catch (RuntimeException exception) {
                if (failure == null)
                    failure = new IllegalStateException("Failed to close project session");
                failure.addSuppressed(exception);
            } finally {
                module = null;
            }
        }
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception exception) {
                if (failure == null)
                    failure = new IllegalStateException("Failed to close project session");
                failure.addSuppressed(exception);
            }
        }
        resources.clear();
        if (failure != null)
            throw failure;
    }
}
