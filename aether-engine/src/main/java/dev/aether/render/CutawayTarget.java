package dev.aether.render;

import dev.aether.ecs.Component;

/** Protects the entity's transformed unit-box bounds with extra world-space padding. */
public record CutawayTarget(double padding) implements Component {
    public CutawayTarget {
        if (!Double.isFinite(padding) || padding < 0)
            throw new IllegalArgumentException("padding must be finite and non-negative");
    }
}
