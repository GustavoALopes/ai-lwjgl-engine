package dev.aether.demo;

import dev.aether.game.GameContext;
import dev.aether.game.GameModule;

public final class ShowcaseModule implements GameModule {
    @Override public void initialize(GameContext context) {
        DemoScenes.populateShowcase(context.world());
    }
}
