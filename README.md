# Aether Engine

Aether is a Java 25 game-engine foundation with a deterministic, headless-testable core.

## Maven modules

The root `pom.xml` is the parent and reactor aggregator. Import it into your IDE as a Maven project.

| Module | Contents | Aether dependencies |
| --- | --- | --- |
| `aether-engine` | ECS, engine lifecycle, math, physics, scripts, and render/audio component declarations | None |
| `aether-desktop` | GLFW window, desktop loop, OpenGL renderer, textures, and packaged shaders | Engine |
| `examples/tetris` (`aether-tetris`) | Deterministic Tetris game and its ECS presentation adapter | Engine |
| `aether-editor` | ImGui editor, entry point, scene camera, and editor showcase scenes | Engine, desktop, Tetris |

The engine has no production library dependencies and can be built and tested without LWJGL or ImGui. Native dependencies belong to the desktop and editor modules. Each module owns its tests; shaders use the standard `aether-desktop/src/main/resources` directory.

The desktop loop hosts a `DesktopLayer` supplied by the application. It has no dependency on the editor and creates/closes that layer inside the active render context.

This is the first step toward independent game projects. The editor still depends on the Tetris example to preserve the current menus, input, and HUD. Project manifests, scene serialization, runtime game-module loading, and removing that dependency are subsequent steps. These are Maven modules, not Java Platform Module System modules.

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
