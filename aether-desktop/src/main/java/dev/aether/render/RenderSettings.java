package dev.aether.render;

public record RenderSettings(
    boolean hdr,
    boolean ssao,
    boolean softShadows,
    boolean imageBasedLighting,
    double exposure
) {

    public RenderSettings {
        if (!Double.isFinite(exposure) || exposure <= 0)
            throw new IllegalArgumentException("exposure must be positive");
    }

    public static RenderSettings productionDefaults() {
        return new RenderSettings(true, true, true, true, 1);
    }
}
