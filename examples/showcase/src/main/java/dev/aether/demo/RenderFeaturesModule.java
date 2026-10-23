package dev.aether.demo;

import dev.aether.game.GameContext;
import dev.aether.game.GameModule;

public final class RenderFeaturesModule implements GameModule {
    @Override public void initialize(GameContext context) {
        RenderFeatureScene.open(context.world());
    }
}
