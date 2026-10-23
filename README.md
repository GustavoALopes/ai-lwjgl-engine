# Aether Engine

Aether is a Java 25 game-engine foundation with a deterministic, headless-testable core.

## Maven modules

The root `pom.xml` is the parent and reactor aggregator. Import it into your IDE as a Maven project.

| Module | Contents | Aether dependencies |
| --- | --- | --- |
| `aether-engine` | ECS, lifecycle, physics, scripts, game-module API, and declarative components | None |
| `aether-project` | Project manifests, session ownership, and deferred switching | Engine |
| `aether-desktop` | GLFW window, desktop loop, OpenGL renderer, textures, and packaged shaders | Engine |
| `examples/tetris` (`aether-tetris`) | Deterministic Tetris game and its ECS presentation adapter | Engine |
| `examples/showcase` (`aether-showcase`) | Cube showcase and render-feature lab game modules | Engine |
| `aether-editor` | Generic ImGui editor, entry point, scene camera, and asset browser | Engine, project, desktop |

The engine has no production library dependencies and can be built and tested without LWJGL or ImGui. Native dependencies belong to the desktop and editor modules. Each module owns its tests; shaders use the standard `aether-desktop/src/main/resources` directory.

The desktop loop hosts a `DesktopApplication` supplied by the application. It has no dependency on the editor and runs updates, rendering, and shutdown inside the active render context. The earlier `DesktopLayer` overload remains available for applications with a fixed engine.

The editor has no dependency on either example. The headless `aether-project` module uses Gson for manifest JSON and the engine for isolated sessions and game-module loading; it has no desktop or editor dependencies. Game code depends on the engine's public API and loads from the JAR declared by a manifest. These are Maven modules, not Java Platform Module System modules.

## Project files

An `.aetherproj` file stores project configuration as versioned UTF-8 JSON. The example at `examples/tetris/Tetris.aetherproj` declares:

```json
{
  "formatVersion": 1,
  "name": "Tetris",
  "assetDirectory": "assets",
  "gameModule": {
    "jar": "target/aether-tetris.jar",
    "entryPoint": "dev.aether.demo.TetrisModule"
  }
}
```

All configured paths are relative to the manifest's directory. Moving the project folder therefore preserves their meaning. An optional `gameModule` initializes the project from compiled Java code. Serialized scene loading remains separate work.

Build with `mvn install`, then use **File > Open Project...** or **Ctrl+O** and enter a manifest path. Opening `examples/tetris/Tetris.aetherproj` loads Tetris directly. Click Scene View to send it keyboard input. The other examples are `examples/showcase/Showcase.aetherproj` and `examples/showcase/RenderFeatures.aetherproj`.

**New Project** creates a minimal manifest and assets directory without overwriting an existing manifest. **Recent Projects** keeps the last ten opened projects for this editor run. **Reload Project** rereads the manifest/JAR, and **Close Project** releases the session. **New Scene** creates an empty world within the current project; Reload restores its declared module. Each replacement uses a fresh engine/world and module class loader.

The Asset Browser lists files and directories from the active project's asset root, with folder navigation and Refresh. Game input, HUD data, and material colors come from the game module through public engine APIs.

Editor scene edits trigger a discard/cancel prompt before opening, closing, resetting, or quitting. Scene saving is not implemented, so Open Scene and Save Scene remain disabled. Failed project opens preserve the current scene and report their error in Console.

See [the project format](docs/project-files.md), [session lifecycle](docs/project-sessions.md), and [game module API](docs/game-modules.md) for details.

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
mvn -pl aether-editor exec:java "-Dexec.args=--headless examples/tetris/Tetris.aetherproj"
```

The install step is needed because this second Maven invocation selects only the editor module. Rerun `mvn install` after changing its sibling modules.

## Current foundation

- Ordered ECS with stable entity handles and deferred structural changes during system updates.
- Transform and rigid-body components with deterministic gravity integration.
- Script lifecycle API and explicit engine lifecycle states.
- OpenGL rendering with HDR tone mapping, environment lighting, soft contact shadows, and SSAO-style contact occlusion.
- ImGui menus, hierarchy, Inspector, Scene View, Asset Browser, and Console.
- Independent showcase and Tetris projects loaded through manifests.
