# Independent game modules

Games depend on `aether-engine`, not the editor. A project's manifest declares a compiled JAR and a public `GameModule` entry point with a public no-argument constructor. Each session receives its own class loader and game instance.

## Minimal game

```java
package games.example;

import dev.aether.game.GameContext;
import dev.aether.game.GameModule;
import dev.aether.math.Vec3;
import dev.aether.render.Material;
import dev.aether.render.MeshRenderer;
import dev.aether.scene.Transform;

public final class ExampleGame implements GameModule {
    @Override
    public void initialize(GameContext context) {
        var cube = context.world().create("My cube");
        context.world().add(cube, Transform.identity());
        context.world().add(cube, new MeshRenderer("primitive/cube", "game/blue"));
        context.world().add(cube, new Material(new Vec3(.1, .4, 1), 0));
    }
}
```

Compile the game against `dev.aether:aether-engine` using the engine's version and Java 25. Configure Maven's `finalName` if you want a stable JAR filename, then declare it in the project's `.aetherproj`:

```json
{
  "formatVersion": 1,
  "name": "Example Game",
  "assetDirectory": "assets",
  "gameModule": {
    "jar": "target/my-game.jar",
    "entryPoint": "games.example.ExampleGame"
  }
}
```

The assets directory must exist. Modules may initialize an entire scene in Java, as the examples do; no serialized scene file is required.

Keep constructors free of resource acquisition. In `initialize(context)`, register systems/components with `context.world()`, locate project assets with `context.assetDirectory()`, and immediately transfer acquired resource ownership through `context.own(resource)`. The host starts the engine only after initialization succeeds.

On shutdown the world and scripts close first, followed by `GameModule.close()`, then owned resources in reverse acquisition order, and finally the class loader. The module itself is already host-owned and must not be registered through `context.own`.

## Input and game UI

Override `input(GameInput)` to receive immutable logical key snapshots. `held()` contains keys currently down; `pressed()` contains rising edges. Games decide what those keys mean.

The editor sends keyboard input to the focused Scene View, suspends delivery while typing or using project dialogs, and suppresses held keys until release after input capture changes. Pause suspends game input and world updates. The engine API has no GLFW dependency; `LwjglGameKeys` is the desktop adapter.

Override `presentation()` to return a `GamePresentation`: a default 2D/3D view mode, immutable HUD labels, and a controls hint. Label positions use normalized viewport coordinates in `[0, 1]`; colors use `0xRRGGBBAA`. The editor draws these generic labels over the game viewport. The game never imports ImGui.

`Material` is an immutable ECS component supplying a linear RGB tint and emissive intensity. HDR colors can exceed one. Game-specific material IDs and color palettes stay in the game project.

## Build, open, and reload

From the engine repository:

```shell
mvn install
mvn -pl aether-editor exec:java
```

Use **File > Open Project...** for:

- `examples/tetris/Tetris.aetherproj`
- `examples/showcase/Showcase.aetherproj`
- `examples/showcase/RenderFeatures.aetherproj`

Opening these manifests initializes their scenes directly. Click Scene View to give the game keyboard focus.

Rebuild a game's JAR and use **Reload Project** to load a fresh module instance. Automatic compilation and hot reload are not implemented. Version 1 expects the game's dependencies to be available to its class loader; the supplied examples need only the shared engine API.

A failed load preserves the active project. Class-loading and initialization failures close the prepared session, its partial resources, and the new class loader. Old loaders close after successful switches. Live game callback errors pause simulation and appear in Console.

For headless validation of the packaged Tetris game:

```shell
mvn -pl aether-editor exec:java "-Dexec.args=--headless examples/tetris/Tetris.aetherproj"
```

This loads and initializes the actual game JAR, dispatches an empty input snapshot, reads its presentation, performs an update, and closes its session without a native context.

## Asset browser and remaining work

The Asset Browser reads directory snapshots from the active project's asset root. It lists directories first in a stable order, supports folder navigation and refresh, and skips symbolic-link entries. It does not import assets or create GPU objects.

Scene serialization, a separate cloned Play world, persistent editor preferences/recent history, automatic game builds, and richer mouse/controller/UI APIs remain future extensions.
