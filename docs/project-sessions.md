# Project sessions and switching

`ProjectManager` owns at most one active `ProjectSession`. Each session owns a fresh `AetherEngine`, its world and systems, and explicitly registered resources. Sessions can represent either a manifest-backed project or an untitled editor scene.

## Host integration

```java
try (var projects = new ProjectManager()) {
    projects.requestOpen(Path.of("examples/tetris/Tetris.aetherproj"));

    // Call between frames on the render thread, with its context active:
    projects.applyPending();
    projects.tick(deltaSeconds);

    projects.activeSession().ifPresent(session -> {
        // Render session.world(), inspect session.project(), etc.
    });
}
```

Requests do not immediately change the active world. `applyPending()` consumes the latest request, and `tick()` updates only the active running engine. Calls to apply a transition, close the manager, or recursively tick during a world update are rejected. A system may queue a request during update; it takes effect at the next boundary.

The API is single-threaded. Hosts must also keep transitions outside rendering and UI iteration. The editor applies its queued actions in `DesktopApplication.update()`, before ticking and before building the next ImGui frame.

## Preparation and activation

For an open request the manager:

1. Reads and validates the manifest.
2. Requires its asset directory and any declared starting-scene/JAR files to exist.
3. Creates a prepared session with a fresh engine/world.
4. Calls the host's initializer, then starts the new engine.
5. Publishes the new active session and closes the previous one.

Failures before publication close the candidate's acquired resources and leave the previous engine, pause state, entities, and systems intact. A failed request is consumed; the caller can correct the problem and queue another request.

The default initializer opens minimal manifests as empty scenes. It explicitly rejects manifests declaring a starting scene or game module because those loaders are not implemented yet. Hosts can provide a `ProjectInitializer` to add scene/module support:

```java
var projects = new ProjectManager(session -> {
    var project = session.project().orElseThrow();
    // Acquire a resource, immediately transfer ownership with session.own(resource),
    // then register game systems/components in session.world().
    // Leave session.engine() in NEW state; the manager starts it after preparation.
});
```

`requestReset(initializer)` replaces the world with a fresh session while preserving the active project's configuration. With no active project it creates an untitled session. `requestClose()` clears the active session at the next boundary. Closing the manager discards pending requests and closes its active session.

## Shutdown and resource ownership

`session.own(resource)` registers an `AutoCloseable`. Ownership transfers only when registration succeeds; registering the same object twice or registering after closure is rejected.

On shutdown the session first closes its engine/world. World systems close in reverse registration order through `WorldSystem.close(world)`; `ScriptSystem` invokes `onDestroy` for scripts it initialized, before world data is cleared. The session then closes registered resources in reverse acquisition order. Register a class loader before the resources that depend on it, so it closes after them and after script teardown.

Normal cleanup exceptions do not prevent the remaining cleanup callbacks from running. They are collected as suppressed exceptions. Closure is idempotent, including after a cleanup failure. A closed world rejects updates and mutations, and releases its entities, components, systems, and deferred changes.

If cleanup of the previous session fails after publication, the new session stays active and the cleanup error is reported. Rolling back at this point would expose a partially destroyed old session.

The editor closes sessions before disposing ImGui and before the desktop runtime releases its shared renderer and window/context. The shared renderer's framebuffer, built-in shaders, and checker texture have application lifetime; future project-specific GPU assets should be registered with their owning session.

## Editor behavior

- **Open Project... / Ctrl+O** opens an ImGui path dialog for `.aetherproj` files.
- **Close Project** releases the active session, leaving the editor open.
- **New Scene / Ctrl+N** creates an empty world in the current project, or an untitled session.
- Game demo menu actions also replace the whole session world; Tetris systems cannot leak into the showcase.
- Each successful change resets selection, inspector selection, dragging, held game keys, navigation state, and camera.
- Authored scene edits trigger a discard/cancel prompt for replacement or quitting, including the window close button. Cancelling preserves the scene; a failed open preserves its dirty state even after the user agreed to discard.
- Scene save/load and dynamic game-module loading remain unavailable. Opening the Tetris manifest sets the project context; choose **Game > Open Tetris Demo** to start its existing Java scene.

The editor's `EditorProjects` controller handles queuing, confirmations, demo metadata, and errors without ImGui or a native context, so these behaviors have headless JUnit coverage.
