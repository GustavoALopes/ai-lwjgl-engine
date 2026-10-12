# Aether development guide

- Keep the engine deterministic: systems run in registration order and structural ECS changes are deferred during iteration.
- Validate public arguments and maintain lifecycle invariants; prefer immutable snapshots at API boundaries.
- The core must remain headless-testable. GLFW/OpenGL/OpenAL are adapters, never dependencies of ECS tests.
- Add or update JUnit tests for every behavior change. Run `mvn test` before committing.
- Rendering resources must be explicitly released; never create OpenGL objects outside an active render context.
- Do not commit generated directories (`target/`, IDE metadata) or secrets.
