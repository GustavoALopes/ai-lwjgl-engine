package dev.aether.render;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaderSourceTest {
    @Test
    void loadsPackagedEngineShaders() {
        assertTrue(ShaderSource.load("/shaders/engine/engine.vert").startsWith("#version 410 core"));
        assertTrue(ShaderSource.load("/shaders/engine/engine.frag").contains("uniform sampler2D tex"));
        assertTrue(ShaderSource.load("/shaders/engine/post.vert").contains("gl_VertexID"));
        assertTrue(ShaderSource.load("/shaders/engine/post.frag").contains("uniform float exposure"));
    }

    @Test
    void rejectsMissingShaderResources() {
        assertThrows(IllegalArgumentException.class, () -> ShaderSource.load("/shaders/engine/missing.frag"));
    }
}
