package dev.aether.editor;

import dev.aether.demo.DemoScenes;
import dev.aether.demo.RenderFeatureScene;
import dev.aether.demo.TetrisDemo;
import dev.aether.demo.TetrisScene;
import dev.aether.project.ProjectManager;
import dev.aether.project.ProjectSession;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Headless editor project controller: queued actions, unsaved edits, and demo presentation state. */
public final class EditorProjects implements AutoCloseable {
    public enum Scene { EMPTY, SHOWCASE, TETRIS, RENDER_FEATURES }

    private final ProjectManager manager;
    private Action pending;
    private Action confirmation;
    private Scene scene = Scene.EMPTY;
    private TetrisScene tetris;
    private boolean dirty;
    private boolean quit;
    private String error;
    private boolean closed;

    public EditorProjects() {
        manager = new ProjectManager();
        requestScene(Scene.SHOWCASE);
        applyPending();
    }

    public Optional<ProjectSession> activeSession() { return manager.activeSession(); }
    public Scene scene() { return scene; }
    public Optional<TetrisScene> tetris() { return Optional.ofNullable(tetris); }
    public boolean dirty() { return dirty; }
    public boolean needsConfirmation() { return confirmation != null; }
    public boolean quitRequested() { return quit; }
    public Optional<String> error() { return Optional.ofNullable(error); }

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

    public void requestScene(Scene scene) {
        request(new ChangeScene(Objects.requireNonNull(scene, "scene")));
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
        manager.tick(deltaSeconds);
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
        Scene nextScene = action instanceof ChangeScene change ? change.scene() : Scene.EMPTY;
        TetrisScene[] preparedTetris = {null};
        try {
            if (action instanceof Open open)
                manager.requestOpen(open.path());
            else if (action instanceof Close)
                manager.requestClose();
            else
                manager.requestReset(session -> {
                    switch (nextScene) {
                        case SHOWCASE -> DemoScenes.populateShowcase(session.world());
                        case RENDER_FEATURES -> RenderFeatureScene.open(session.world());
                        case TETRIS -> {
                            preparedTetris[0] = new TetrisScene(new TetrisDemo(0L));
                            preparedTetris[0].open(session.world());
                        }
                        case EMPTY -> { }
                    }
                });
            manager.applyPending();
            error = null;
        } catch (IOException | RuntimeException failure) {
            error = failure.getMessage();
        } finally {
            if (previous != activeSession().orElse(null)) {
                scene = nextScene;
                tetris = preparedTetris[0];
                dirty = false;
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
        tetris = null;
        manager.close();
    }

    private void requireOpen() {
        if (closed)
            throw new IllegalStateException("Editor projects are closed");
    }

    private sealed interface Action permits Open, ChangeScene, Close, Quit { }
    private record Open(Path path) implements Action { }
    private record ChangeScene(Scene scene) implements Action { }
    private record Close() implements Action { }
    private record Quit() implements Action { }
}
