# Aether Engine

Aether is a Java 25 game-engine foundation with a deterministic, headless-testable core.

## Maven modules

The root `pom.xml` is the parent and reactor aggregator. Import it into your IDE as a Maven project.

| Module | Contents | Aether dependencies |
| --- | --- | --- |
| `aether-engine` | ECS, engine lifecycle, math, physics, scripts, and render/audio component declarations | None |
| `aether-project` | Project manifests, session ownership, and deferred switching | Engine |
| `aether-desktop` | GLFW window, desktop loop, OpenGL renderer, textures, and packaged shaders | Engine |
| `examples/tetris` (`aether-tetris`) | Deterministic Tetris game and its ECS presentation adapter | Engine |
| `aether-editor` | ImGui editor, entry point, scene camera, and editor showcase scenes | Engine, project, desktop, Tetris |

The engine has no production library dependencies and can be built and tested without LWJGL or ImGui. Native dependencies belong to the desktop and editor modules. Each module owns its tests; shaders use the standard `aether-desktop/src/main/resources` directory.

The desktop loop hosts a `DesktopApplication` supplied by the application. It has no dependency on the editor and runs updates, rendering, and shutdown inside the active render context. The earlier `DesktopLayer` overload remains available for applications with a fixed engine.

The editor still depends on the Tetris example to preserve the current game menus, input, and HUD. The headless `aether-project` module uses Gson for manifest JSON and the engine for isolated project sessions; it has no desktop or editor dependencies. The Tetris module uses it only in tests. Scene serialization and runtime game-module loading are subsequent steps. These are Maven modules, not Java Platform Module System modules.

## Project files

An `.aetherproj` file stores project configuration as versioned UTF-8 JSON. The example at `examples/tetris/Tetris.aetherproj` currently declares:

```json
{
  "formatVersion": 1,
  "name": "Tetris",
  "assetDirectory": "assets"
}
```

All configured paths are relative to the manifest's directory. Moving the project folder therefore preserves their meaning. Optional starting-scene and game-module declarations are supported by the format, but do not load scenes or execute code yet.

Use **File > Open Project...** or **Ctrl+O** and enter a manifest path. The dialog shows the base directory used for relative paths. Opening `examples/tetris/Tetris.aetherproj` creates an empty scene in that project's context; use **Game > Open Tetris Demo** to play the existing Java demo. Its manifest does not yet declare a serialized scene or loadable game module.

**File > Close Project** releases the session. **New Scene** creates an empty world, keeping the current project if one is open. The Game menu also offers the cube showcase and render-feature lab. Each switch uses a fresh engine/world, preventing old demo systems from running in the new scene.

Editor scene edits trigger a discard/cancel prompt before opening, closing, resetting, or quitting. Scene saving is not implemented, so Open Scene and Save Scene remain disabled. Failed project opens preserve the current scene and report their error in Console.

See [the project file format and Java API](docs/project-files.md) and [session lifecycle](docs/project-sessions.md) for details.

## Build and test

Install JDK 25 and Maven 3.9+, then run from the repository root:

```shell
mvn test
```

To test only the core, or Tetris and its required modules:

```shell
mvn -pl aether-engine test
mvn -pl examples/tetris -am test
```

To package all modules as JARs:

```shell
mvn package
```

On Windows, if Maven is available only through Git Bash, use:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' -lc 'mvn test'
```

## Run the editor

In your IDE, run `dev.aether.Main` using the `aether-editor` module's runtime classpath. Without arguments it opens the desktop editor; `--headless` exercises the engine lifecycle without creating a window.

For command-line execution, first install the reactor artifacts into your local Maven repository, then run the editor module:

```shell
mvn install
mvn -pl aether-editor exec:java
```

For a headless smoke run:

```shell
mvn -pl aether-editor exec:java -Dexec.args=--headless
```

The install step is needed because this second Maven invocation selects only the editor module. Rerun `mvn install` after changing its sibling modules.

## Current foundation

- Ordered ECS with stable entity handles and deferred structural changes during system updates.
- Transform and rigid-body components with deterministic gravity integration.
- Script lifecycle API and explicit engine lifecycle states.
- OpenGL rendering with HDR tone mapping, environment lighting, soft contact shadows, and SSAO-style contact occlusion.
- ImGui menus, hierarchy, Inspector, Scene View, Asset Browser, and Console.
- Editor showcase scenes and a deterministic Tetris example.
