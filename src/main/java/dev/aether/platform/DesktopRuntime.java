package dev.aether.platform;

import dev.aether.core.AetherEngine;
import dev.aether.render.OpenGlRenderer;
import java.util.Objects;

/** Desktop frame loop, isolated from the deterministic engine core for CI and editor automation. */
public final class DesktopRuntime {
    private DesktopRuntime() { }
    public static void run(AetherEngine engine, WindowConfig config, OpenGlRenderer renderer) {
        Objects.requireNonNull(engine); Objects.requireNonNull(config); Objects.requireNonNull(renderer);
        try (LwjglWindow window = new LwjglWindow(config)) {
            engine.start();
            long previous = System.nanoTime();
            while (!window.shouldClose()) {
                long now = System.nanoTime();
                engine.tick((now - previous) / 1_000_000_000.0);
                previous = now;
                renderer.render(window);
                window.present();
                window.pollEvents();
            }
            engine.stop();
        } finally { engine.close(); }
    }
}
