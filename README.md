# Aether Engine

Aether is a Java 25 game-engine foundation with a deterministic, headless-testable core and LWJGL platform dependencies prepared for GLFW, OpenGL, OpenAL, STB Vorbis, and Assimp.

## Current foundation

- Ordered ECS with stable entity handles and deferred structural changes during system updates.
- Transform and rigid-body components with deterministic gravity integration.
- Script lifecycle API (`onCreate`, `onUpdate`, `onDestroy`); the host is deliberately language-neutral for the future Lua adapter.
- Explicit engine state machine and headless launch path suited to automated editor agents.
- Render-feature contract for HDR, SSAO, soft shadows, and IBL; OpenGL backend work can be built without coupling it to core tests.

## Build

Install Maven 3.9+ and run:

```powershell
mvn test
```

The repository targets Java 25. `dev.aether.Main` is intentionally headless at this stage, so it is safe to use in CI.
