package dev.aether.game;

import java.util.List;
import java.util.Objects;

/** Immutable game-viewport overlay. Positions are normalized and colors use 0xRRGGBBAA. */
public record GamePresentation(ViewMode viewMode, List<Label> labels, String controls) {
    public enum ViewMode { TWO_D, THREE_D }

    public GamePresentation {
        Objects.requireNonNull(viewMode, "viewMode");
        labels = List.copyOf(Objects.requireNonNull(labels, "labels"));
        Objects.requireNonNull(controls, "controls");
    }

    public static GamePresentation empty() {
        return new GamePresentation(ViewMode.THREE_D, List.of(), "");
    }

    public record Label(double x, double y, String text, int rgba) {
        public Label {
            if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 1 || y < 0 || y > 1)
                throw new IllegalArgumentException("Label coordinates must be within [0, 1]");
            Objects.requireNonNull(text, "text");
        }
    }
}
