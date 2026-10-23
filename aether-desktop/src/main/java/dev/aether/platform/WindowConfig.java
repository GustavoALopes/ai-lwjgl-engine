package dev.aether.platform;

import java.util.Objects;

/**
 * Immutable desktop-window request; valid independently from a GLFW
 * installation.
 */
public record WindowConfig(String title, int width, int height, boolean resizable, boolean vSync) {

    public WindowConfig {
        Objects.requireNonNull(title, "title");
        if (title.isBlank() || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("title and positive dimensions are required");
        }
    }

    public static WindowConfig editorDefaults() {
        return new WindowConfig("Aether Editor", 1600, 900, true, true);
    }
}
