package dev.aether.render;

import dev.aether.ecs.Component;

/** Renderer-facing intensity for a short gameplay-driven full-screen pulse. */
public record PostProcessPulse(double intensity) implements Component {
    public PostProcessPulse {
        if (!Double.isFinite(intensity) || intensity < 0 || intensity > 1) throw new IllegalArgumentException("intensity must be finite and between zero and one");
    }
}
