package dev.aether.platform;

import dev.aether.Aether;
import dev.aether.core.AetherEngine;
import dev.aether.render.OpenGlRenderer;
import dev.aether.render.RenderSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DesktopRuntimeTest {
    @Test void sessionApplicationRejectsMissingArgumentsBeforeOpeningAWindow() {
        var config = WindowConfig.editorDefaults();
        var renderer = new OpenGlRenderer(RenderSettings.productionDefaults());
        DesktopApplication.Factory factory = (window, r) -> {
            fail("Validation must happen before creating the application");
            return null;
        };
        assertThrows(NullPointerException.class, () -> DesktopRuntime.run(null, renderer, factory));
        assertThrows(NullPointerException.class, () -> DesktopRuntime.run(config, null, factory));
        assertThrows(NullPointerException.class, () -> DesktopRuntime.run(config, renderer, null));
    }
    @Test void windowConfigRejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new WindowConfig("", 0, 1, true, true));
    }

    @Test void rejectsMissingArgumentsBeforeOpeningAWindow() {
        try (var engine = Aether.createEngine()) {
            var config = WindowConfig.editorDefaults();
            var renderer = new OpenGlRenderer(RenderSettings.productionDefaults());
            DesktopLayer.Factory factory = (window, e, r) -> {
                fail("Validation must happen before creating the application layer");
                return null;
            };
            assertThrows(NullPointerException.class, () -> DesktopRuntime.run(null, config, renderer, factory));
            assertThrows(NullPointerException.class, () -> DesktopRuntime.run(engine, null, renderer, factory));
            assertThrows(NullPointerException.class, () -> DesktopRuntime.run(engine, config, null, factory));
            assertThrows(NullPointerException.class, () -> DesktopRuntime.run(engine, config, renderer, null));
            assertEquals(AetherEngine.State.NEW, engine.state());
        }
    }
}
