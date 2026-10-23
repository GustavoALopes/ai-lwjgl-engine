package dev.aether.editor;

import dev.aether.game.GameInput;
import dev.aether.game.GamePresentation;
import dev.aether.core.AetherEngine;
import dev.aether.project.ProjectManager;
import dev.aether.project.ProjectSession;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

/** Headless editor controller: queued project actions, unsaved edits, and generic game dispatch. */
public final class EditorProjects implements AutoCloseable {
    private final ProjectManager manager;
    private Action pending;
    private Action confirmation;
    private boolean dirty;
    private boolean quit;
    private String error;
    private boolean closed;
    private List<Path> recent = List.of();

    public EditorProjects() {
        manager = new ProjectManager();
        requestNewScene();
        applyPending();
    }

    public Optional<ProjectSession> activeSession() { return manager.activeSession(); }
    public boolean dirty() { return dirty; }
    public boolean needsConfirmation() { return confirmation != null; }
    public boolean quitRequested() { return quit; }
    public Optional<String> error() { return Optional.ofNullable(error); }
    public List<Path> recentProjects() { return recent; }

    public void markDirty() {
        requireOpen();
        if (activeSession().isPresent())
            dirty = true;
        if (pending != null) {
            confirmation = pending;
            pending = null;
        }
    }

    public void requestOpen(Path path) {
        request(new Open(Objects.requireNonNull(path, "path").toAbsolutePath().normalize()));
    }

    public void requestCreate(Path path, String name) {
        dev.aether.project.ProjectConfig.create(name, Path.of("assets"));
        request(new Create(Objects.requireNonNull(path, "path").toAbsolutePath().normalize(), name));
    }

    public void requestNewScene() { request(new NewScene()); }

    public void requestReload() {
        Path manifest = activeSession().flatMap(ProjectSession::project)
                .orElseThrow(() -> new IllegalStateException("No project is open")).manifestFile();
        requestOpen(manifest);
    }

    public void input(GameInput input) {
        requireOpen();
        Objects.requireNonNull(input, "input");
        activeSession().filter(session -> session.engine().state() == AetherEngine.State.RUNNING)
                .flatMap(ProjectSession::module).ifPresent(module -> {
                    try { module.input(input); }
                    catch (RuntimeException failure) { reportGameFailure(failure); }
                });
    }

    public GamePresentation presentation() {
        requireOpen();
        try {
            return activeSession().flatMap(ProjectSession::module)
                    .map(module -> Objects.requireNonNull(module.presentation(), "Game presentation"))
                    .orElseGet(GamePresentation::empty);
        } catch (RuntimeException failure) {
            reportGameFailure(failure);
            return GamePresentation.empty();
        }
    }

    public void requestClose() { request(new Close()); }
    public void requestQuit() { request(new Quit()); }

    public void discardAndContinue() {
        requireOpen();
        if (confirmation == null)
            throw new IllegalStateException("No action is awaiting confirmation");
        pending = confirmation;
        confirmation = null;
    }

    public void cancel() { requireOpen(); confirmation = null; }

    public void update(double deltaSeconds) {
        requireOpen();
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0)
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        applyPending();
        try { manager.tick(deltaSeconds); }
        catch (RuntimeException failure) { reportGameFailure(failure); }
    }

    private void reportGameFailure(RuntimeException failure) {
        error = "Game error: " + failure;
        activeSession().map(ProjectSession::engine)
                .filter(engine -> engine.state() == AetherEngine.State.RUNNING)
                .ifPresent(AetherEngine::pause);
    }

    private void request(Action action) {
        requireOpen();
        if (dirty) {
            confirmation = action;
            pending = null;
        } else {
            pending = action;
            confirmation = null;
        }
    }

    private void applyPending() {
        Action action = pending;
        if (action == null)
            return;
        pending = null;
        if (action instanceof Quit) {
            quit = true;
            return;
        }
        ProjectSession previous = activeSession().orElse(null);
        try {
            if (action instanceof Open open)
                manager.requestOpen(open.path());
            else if (action instanceof Create create)
                manager.requestCreate(create.path(), create.name());
            else if (action instanceof Close)
                manager.requestClose();
            else
                manager.requestReset(session -> { });
            manager.applyPending();
            error = null;
        } catch (IOException | RuntimeException failure) {
            error = failure.getMessage();
        } finally {
            if (previous != activeSession().orElse(null)) {
                dirty = false;
                activeSession().flatMap(ProjectSession::project).ifPresent(project -> {
                    var next = new ArrayList<>(recent);
                    next.remove(project.manifestFile());
                    next.addFirst(project.manifestFile());
                    recent = List.copyOf(next.subList(0, Math.min(10, next.size())));
                });
            }
        }
    }

    @Override
    public void close() {
        if (closed)
            return;
        if (activeSession().map(session -> session.world().isUpdating()).orElse(false))
            throw new IllegalStateException("Cannot close editor projects during update");
        closed = true;
        pending = null;
        confirmation = null;
        manager.close();
    }

    private void requireOpen() {
        if (closed)
            throw new IllegalStateException("Editor projects are closed");
    }

    private sealed interface Action permits Open, Create, NewScene, Close, Quit { }
    private record Open(Path path) implements Action { }
    private record Create(Path path, String name) implements Action { }
    private record NewScene() implements Action { }
    private record Close() implements Action { }
    private record Quit() implements Action { }
}
