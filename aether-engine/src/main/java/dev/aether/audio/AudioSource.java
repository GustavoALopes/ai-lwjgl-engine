package dev.aether.audio;

import dev.aether.ecs.Component;
import java.util.Objects;

/** Spatial audio request consumed by the OpenAL adapter. */
public record AudioSource(AudioClip clip, boolean looping, double gain) implements Component {
    public AudioSource { Objects.requireNonNull(clip); if (!Double.isFinite(gain) || gain < 0) throw new IllegalArgumentException("gain must be non-negative"); }
}
