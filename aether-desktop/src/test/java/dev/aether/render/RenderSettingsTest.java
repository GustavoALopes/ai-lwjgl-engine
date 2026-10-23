package dev.aether.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderSettingsTest {
    @Test void productionFeatureConfigurationEnablesTheFeaturePipeline() {
        RenderSettings settings = RenderSettings.productionDefaults();
        assertTrue(settings.hdr());
        assertTrue(settings.ssao());
        assertTrue(settings.softShadows());
        assertTrue(settings.imageBasedLighting());
        assertEquals(1, settings.exposure());
    }
}
