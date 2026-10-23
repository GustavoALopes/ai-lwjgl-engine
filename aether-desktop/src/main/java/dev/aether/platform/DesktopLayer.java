package dev.aether.platform;

import dev.aether.core.AetherEngine;
import dev.aether.render.OpenGlRenderer;

/** Application UI hosted by the desktop loop, created and closed inside its render context. */
public interface DesktopLayer extends AutoCloseable {
    void render(LwjglWindow window, AetherEngine engine);

    @Override
    void close();

    @FunctionalInterface
    interface Factory {
        DesktopLayer create(LwjglWindow window, AetherEngine engine, OpenGlRenderer renderer);
    }
}
