package dev.aether.render;

import dev.aether.game.GamePresentation.Anchor;
import java.util.Objects;

/** Pure viewport layout in logical pixels; independent of cameras and native code. */
public final class HudLayout {
    private HudLayout() { }

    public record Panel(float x, float y, float width, float height, float scale) { }

    public static Panel panel(Anchor anchor, float viewportWidth, float viewportHeight, float contentWidth) {
        Objects.requireNonNull(anchor, "anchor");
        if (!Float.isFinite(viewportWidth) || !Float.isFinite(viewportHeight)
                || !Float.isFinite(contentWidth) || viewportWidth <= 0 || viewportHeight <= 0 || contentWidth < 0)
            throw new IllegalArgumentException("Viewport must be positive and content width nonnegative and finite");
        float width = Math.max(anchor == Anchor.TOP_LEFT ? 75 : 125, contentWidth + 32);
        // Fit both initial cards in narrow viewports without overlap, preserving centered score.
        float scale = Math.min(1, Math.min(viewportWidth / Math.max(340, width * 3 + 32), viewportHeight / 118));
        float x = anchor == Anchor.TOP_LEFT ? 12 * scale : (viewportWidth - width * scale) / 2;
        return new Panel(x, 12 * scale, width * scale, 96 * scale, scale);
    }
}
