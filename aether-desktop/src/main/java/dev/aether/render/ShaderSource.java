package dev.aether.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Loads GLSL source packaged with the engine. */
final class ShaderSource {
    private ShaderSource() {
    }

    static String load(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        try (InputStream stream = ShaderSource.class.getResourceAsStream(resourcePath)) {
            if (stream == null)
                throw new IllegalArgumentException("Shader resource not found: " + resourcePath);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read shader resource: " + resourcePath, exception);
        }
    }
}
