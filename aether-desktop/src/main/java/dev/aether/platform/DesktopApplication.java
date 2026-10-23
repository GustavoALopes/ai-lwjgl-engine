package dev.aether.platform;

import dev.aether.render.OpenGlRenderer;

/** Frame-based host whose engines may change during its lifetime. Calls run on the render thread. */
public interface DesktopApplication extends AutoCloseable {
    void update(double deltaSeconds);
    void render(LwjglWindow window);
    default boolean canClose() { return true; }
    @Override void close();

    @FunctionalInterface
    interface Factory {
        DesktopApplication create(LwjglWindow window, OpenGlRenderer renderer);
    }
}
