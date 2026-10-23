package dev.aether.project;

import dev.aether.core.AetherEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Single-threaded session owner. Requests are deferred; the host calls applyPending
 * between frames with its render context active, then tick. The last queued request wins.
 */
public final class ProjectManager implements AutoCloseable {
    private final ProjectSerializer serializer = new ProjectSerializer();
    private final ProjectInitializer initializer;
    private ProjectSession active;
    private Request pending;
    private boolean busy;
    private boolean closed;

    public ProjectManager() {
        this(session -> {
            ProjectFile file = session.project().orElseThrow();
            if (file.startScene().isPresent() || file.gameModuleJar().isPresent())
                throw new IOException("Scene and game-module loading are not configured for this host");
        });
    }

    public ProjectManager(ProjectInitializer initializer) {
        this.initializer = Objects.requireNonNull(initializer, "initializer");
    }

    public Optional<ProjectSession> activeSession() {
        return Optional.ofNullable(active);
    }

    public boolean hasPending() { return pending != null; }

    public void requestOpen(Path manifest) {
        requireOpen();
        pending = new Open(ProjectPaths.manifest(manifest));
    }

    /** Creates a fresh world within the current project, or an untitled session if none is open. */
    public void requestReset(ProjectInitializer sceneInitializer) {
        requireOpen();
        pending = new Reset(Objects.requireNonNull(sceneInitializer, "sceneInitializer"));
    }

    public void requestClose() {
        requireOpen();
        pending = new Close();
    }

    public boolean applyPending() throws IOException {
        requireIdle();
        if (pending == null)
            return false;
        Request request = pending;
        pending = null;
        busy = true;
        try {
            if (request instanceof Close) {
                ProjectSession previous = active;
                active = null;
                if (previous != null)
                    previous.close();
                return previous != null;
            }
            Optional<ProjectFile> file;
            ProjectInitializer prepare;
            if (request instanceof Open open) {
                ProjectFile loaded = serializer.read(open.manifest());
                validateFiles(loaded);
                file = Optional.of(loaded);
                prepare = initializer;
            } else {
                file = active == null ? Optional.empty() : active.project();
                prepare = ((Reset) request).initializer();
            }
            ProjectSession candidate = new ProjectSession(file);
            try {
                prepare.initialize(candidate);
                candidate.activate();
            } catch (IOException | RuntimeException | Error failure) {
                try { candidate.close(); }
                catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
            ProjectSession previous = active;
            active = candidate;
            if (previous != null)
                previous.close();
            return true;
        } finally {
            busy = false;
        }
    }

    /** Updates only the active session. Queued transitions wait for applyPending. */
    public void tick(double deltaSeconds) {
        requireIdle();
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0)
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        busy = true;
        try {
            if (active != null && active.engine().state() == AetherEngine.State.RUNNING)
                active.engine().tick(deltaSeconds);
        } finally {
            busy = false;
        }
    }

    @Override
    public void close() {
        if (closed)
            return;
        requireIdle();
        closed = true;
        pending = null;
        ProjectSession previous = active;
        active = null;
        if (previous != null)
            previous.close();
    }

    private void validateFiles(ProjectFile file) throws IOException {
        if (!Files.isDirectory(file.assetDirectory()))
            throw new IOException("Project asset directory does not exist: " + file.assetDirectory());
        if (file.startScene().isPresent() && !Files.isRegularFile(file.startScene().orElseThrow()))
            throw new IOException("Starting scene does not exist: " + file.startScene().orElseThrow());
        if (file.gameModuleJar().isPresent() && !Files.isRegularFile(file.gameModuleJar().orElseThrow()))
            throw new IOException("Game module JAR does not exist: " + file.gameModuleJar().orElseThrow());
    }

    private void requireOpen() {
        if (closed)
            throw new IllegalStateException("Project manager is closed");
    }

    private void requireIdle() {
        requireOpen();
        if (busy || (active != null && active.world().isUpdating()))
            throw new IllegalStateException("Session changes are only allowed between updates");
    }

    private sealed interface Request permits Open, Reset, Close { }
    private record Open(Path manifest) implements Request { }
    private record Reset(ProjectInitializer initializer) implements Request { }
    private record Close() implements Request { }
}
