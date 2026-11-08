package dev.aether.render;

import dev.aether.game.GamePresentation.Anchor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test void anchorsCardsToViewportAndFitsNarrowAndShortViews() {
        for (float width : new float[] { 100, 340, 920, 1920 }) {
            for (float height : new float[] { 40, 600 }) {
                var stage = HudLayout.panel(Anchor.TOP_LEFT, width, height, 42);
                var score = HudLayout.panel(Anchor.TOP_CENTER, width, height, 90);
                assertEquals(width / 2, score.x() + score.width() / 2, .001);
                assertTrue(stage.x() >= 0);
                assertTrue(stage.x() + stage.width() < score.x());
                assertTrue(score.x() + score.width() <= width);
                assertTrue(score.y() + score.height() <= height);
            }
        }
    }

    @Test void accommodatesLongScoresAndRejectsInvalidDimensions() {
        var score = HudLayout.panel(Anchor.TOP_CENTER, 920, 600, 450);
        assertTrue(score.width() >= 450 * score.scale());
        assertThrows(IllegalArgumentException.class, () -> HudLayout.panel(Anchor.TOP_LEFT, 0, 100, 10));
        assertThrows(IllegalArgumentException.class, () -> HudLayout.panel(Anchor.TOP_LEFT, 100, Float.NaN, 10));
        assertThrows(IllegalArgumentException.class, () -> HudLayout.panel(Anchor.TOP_LEFT, 100, 100, -1));
        assertThrows(NullPointerException.class, () -> HudLayout.panel(null, 100, 100, 10));
    }
}
