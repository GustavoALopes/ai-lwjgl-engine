# Aether Engine

Aether is a Java 25 game-engine foundation with a deterministic, headless-testable core and LWJGL platform dependencies prepared for GLFW, OpenGL, OpenAL, STB Vorbis, and Assimp.

## Current foundation

- Ordered ECS with stable entity handles and deferred structural changes during system updates.
- Transform and rigid-body components with deterministic gravity integration.
- Script lifecycle API (`onCreate`, `onUpdate`, `onDestroy`); the host is deliberately language-neutral for the future Lua adapter.
- Explicit engine state machine and headless launch path suited to automated editor agents.
- Render-feature contract for HDR, SSAO, soft shadows, and IBL, plus an editor **Game → Open Render Feature Lab** scene. The OpenGL adapter renders an HDR framebuffer with exposure tone mapping, environment lighting, soft contact shadows, and SSAO-style contact occlusion without coupling core tests to OpenGL.
- ImGui editor shell with File/Game/View/Help menus, hierarchy, Inspector, Scene View, Asset Browser, and Console panels.
- A deterministic integrated showcase scene: physics playground, PBR renderer metadata, scripting sandbox, and spatial-audio declaration.

## Build

Install Maven 3.9+ and run:

```powershell
mvn test
```

The repository targets Java 25. `dev.aether.Main` is intentionally headless at this stage, so it is safe to use in CI.

Run `dev.aether.Main` without arguments to open the editor. Use `--headless` for CI. The menu and panel controls are editor-facing views over the same public ECS APIs used by automation.
