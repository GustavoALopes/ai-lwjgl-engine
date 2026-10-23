package dev.aether.game;

import dev.aether.ecs.World;
import java.nio.file.Path;

/** Host services available while initializing a game, independent of any editor or graphics API. */
public interface GameContext {
    World world();
    Path assetDirectory();
    <T extends AutoCloseable> T own(T resource);
}
