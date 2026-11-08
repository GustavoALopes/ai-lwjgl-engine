package dev.aether.game;

import java.util.List;
import java.util.Objects;

/** Immutable game-viewport overlay. Positions are normalized and colors use 0xRRGGBBAA. */
public record GamePresentation(ViewMode viewMode, List<Label> labels, String controls, List<Stat> stats) {
    public enum ViewMode { TWO_D, THREE_D }
    public enum Anchor { TOP_LEFT, TOP_CENTER }

    /** Compatibility constructor for presentations containing only labels. */
    public GamePresentation(ViewMode viewMode, List<Label> labels, String controls) {
        this(viewMode, labels, controls, List.of());
    }

    public GamePresentation {
        Objects.requireNonNull(viewMode, "viewMode");
        labels = List.copyOf(Objects.requireNonNull(labels, "labels"));
        Objects.requireNonNull(controls, "controls");
        stats = List.copyOf(Objects.requireNonNull(stats, "stats"));
    }

    public static GamePresentation empty() {
        return new GamePresentation(ViewMode.THREE_D, List.of(), "");
    }

    /** Display-only values. Simulation rules and mutations belong to the game. */
    public record Stat(Anchor anchor, String label, String value) {
        public Stat {
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(value, "value");
            if (label.isBlank() || value.isBlank())
                throw new IllegalArgumentException("Stat label and value must not be blank");
        }
    }

    public record Label(double x, double y, String text, int rgba) {
        public Label {
            if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 1 || y < 0 || y > 1)
                throw new IllegalArgumentException("Label coordinates must be within [0, 1]");
            Objects.requireNonNull(text, "text");
        }
    }
}
