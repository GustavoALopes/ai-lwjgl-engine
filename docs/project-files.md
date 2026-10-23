# Aether project files, version 1

The `aether-project` Maven module provides immutable project configuration and filesystem persistence without creating a window, world, or render context.

## Format

Project filenames use the `.aetherproj` extension. Contents are UTF-8 JSON:

```json
{
  "formatVersion": 1,
  "name": "My Game",
  "assetDirectory": "assets",
  "startScene": "scenes/main.aetherscene",
  "gameModule": {
    "jar": "target/my-game.jar",
    "entryPoint": "games.example.GameModule"
  }
}
```

This full example illustrates declarations for future scene and code loaders. It does not imply that those files or that class already exist.

| Field | Required | Meaning |
| --- | --- | --- |
| `formatVersion` | Yes | Integer `1`; unsupported versions are rejected |
| `name` | Yes | Nonblank project display name |
| `assetDirectory` | Yes | Project-relative asset directory; `.` means the project root |
| `startScene` | No | Project-relative starting scene file |
| `gameModule` | No | Game code declaration; if present, both fields below are required |
| `gameModule.jar` | With `gameModule` | Project-relative compiled game JAR path |
| `gameModule.entryPoint` | With `gameModule` | Dot-separated ASCII Java class identifiers, including `$` for nested classes |

Omit optional fields when unused. Explicit `null` values are rejected. Fields can appear in any order. Unknown and duplicate fields, incorrect value types, comments, malformed JSON, and trailing content are rejected.

Every path resolves from the directory containing the manifest, including `startScene` and `gameModule.jar`; these do not resolve from `assetDirectory`. Paths are normalized, must remain lexically inside the project directory, and cannot be absolute, UNC, or drive-relative paths. Saves use forward slashes. Windows-style separators are accepted and normalized on input.

Path validation is lexical and does not follow symbolic links. Reading validates declarations only: it does not require referenced files to exist, read scene contents, load a class, or execute game code. Later project-session loading will check the resources it needs.

## Java API

Add a dependency on `dev.aether:aether-project` using the same version as the other Aether artifacts.

```java
import dev.aether.project.ProjectConfig;
import dev.aether.project.ProjectFile;
import dev.aether.project.ProjectSerializer;
import java.nio.file.Path;

var serializer = new ProjectSerializer();
ProjectFile project = serializer.read(Path.of("examples/tetris/Tetris.aetherproj"));

Path root = project.directory();          // Absolute normalized project directory
Path assets = project.assetDirectory();   // Absolute normalized asset directory
var scene = project.startScene();         // Optional absolute path
var jar = project.gameModuleJar();         // Optional absolute path

ProjectConfig config = ProjectConfig.create("My Game", Path.of("assets"));
serializer.write(Path.of("MyGame.aetherproj"), config);
```

`ProjectConfig`, `GameModuleConfig`, and `ProjectFile` are immutable records. The configuration contains portable relative paths; `ProjectFile` anchors it to an absolute manifest location. Loading another manifest creates another snapshot and does not change global state.

Invalid model arguments raise `IllegalArgumentException` (or `NullPointerException` for required null arguments). Invalid manifest contents raise `ProjectFormatException`, an `IOException` containing the manifest path and original cause. Missing files and filesystem failures are reported as `IOException`.

Saving writes the full manifest to a temporary sibling before replacing the destination. Replacement is atomic when supported by the filesystem; otherwise it uses a regular replacement move. Temporary files are removed afterward. The destination's parent directory must already exist.

## Tetris example and next steps

`examples/tetris/Tetris.aetherproj` declares its name and `assets` directory. It omits `startScene` and `gameModule` because the current demo builds its scene in Java and has no loadable game-module contract yet.

This step establishes the file format and API. Project sessions, menu integration, scene persistence, compiled game-module loading, and editor preferences are separate work.
