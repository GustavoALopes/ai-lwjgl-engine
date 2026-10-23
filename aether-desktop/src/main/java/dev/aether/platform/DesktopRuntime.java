package dev.aether.platform;

import dev.aether.core.AetherEngine;
import dev.aether.render.OpenGlRenderer;
import java.util.Objects;

/** Desktop frame loop, isolated from the deterministic engine core for CI and editor automation. */
public final class DesktopRuntime {
    private DesktopRuntime() { }
    /** Hosts session-based applications; application resources close before the render context. */
    public static void run(WindowConfig config, OpenGlRenderer renderer, DesktopApplication.Factory factory) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(factory, "factory");
        try (LwjglWindow window = new LwjglWindow(config); OpenGlRenderer ownedRenderer = renderer;
             DesktopApplication application = Objects.requireNonNull(factory.create(window, ownedRenderer), "application")) {
            long previous = System.nanoTime();
            while (true) {
                if (window.shouldClose()) {
                    if (application.canClose())
                        break;
                    window.cancelClose();
                }
                long now = System.nanoTime();
                application.update((now - previous) / 1_000_000_000.0);
                previous = now;
                application.render(window);
                window.present();
                window.pollEvents();
            }
        }
    }

    public static void run(AetherEngine engine, WindowConfig config, OpenGlRenderer renderer,
                           DesktopLayer.Factory layerFactory) {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(layerFactory, "layerFactory");
        try (LwjglWindow window = new LwjglWindow(config); OpenGlRenderer ownedRenderer = renderer;
             DesktopLayer layer = Objects.requireNonNull(layerFactory.create(window, engine, ownedRenderer), "layer")) {
            try {
                engine.start();
                long previous = System.nanoTime();
                while (!window.shouldClose()) {
                    long now = System.nanoTime();
                    if (engine.state() == AetherEngine.State.RUNNING) engine.tick((now - previous) / 1_000_000_000.0);
                    previous = now;
                    layer.render(window, engine);
                    window.present();
                    window.pollEvents();
                }
            } finally {
                engine.close();
            }
        } finally { engine.close(); }
    }
}
