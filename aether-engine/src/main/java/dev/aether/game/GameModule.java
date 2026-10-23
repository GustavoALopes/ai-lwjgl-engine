package dev.aether.game;

/** Public game entry point. Implementations need a public no-argument constructor. */
public interface GameModule extends AutoCloseable {
    void initialize(GameContext context);
    default void input(GameInput input) { }
    default GamePresentation presentation() { return GamePresentation.empty(); }
    /** Called after world/script teardown, before owned assets and the module loader are released. */
    @Override default void close() { }
}
