package dev.aether.demo;

import dev.aether.game.GameContext;
import dev.aether.game.GameInput;
import dev.aether.game.GameKey;
import dev.aether.game.GameModule;
import dev.aether.game.GamePresentation;
import java.util.List;
import java.util.Locale;

/** All Tetris input, scene initialization, and HUD data live in this project. */
public final class TetrisModule implements GameModule {
    private TetrisScene scene;
    private GameContext context;
    private boolean initialized;
    private boolean closed;

    @Override public void initialize(GameContext context) {
        if (initialized || closed)
            throw new IllegalStateException("Module is already initialized");
        this.context = java.util.Objects.requireNonNull(context, "context");
        initialized = true;
        scene = new TetrisScene(new TetrisDemo(0L));
        scene.open(context.world());
    }

    @Override public void input(GameInput input) {
        requireActive();
        java.util.Objects.requireNonNull(input, "input");
        var game = scene.game();
        var keys = input.pressed();
        if (keys.contains(GameKey.LEFT)) game.moveLeft();
        if (keys.contains(GameKey.RIGHT)) game.moveRight();
        if (keys.contains(GameKey.UP)) game.rotateClockwise();
        if (keys.contains(GameKey.DOWN)) game.softDrop();
        if (keys.contains(GameKey.SPACE)) game.hardDrop();
        if (keys.contains(GameKey.R)) scene.open(context.world());
    }

    @Override public GamePresentation presentation() {
        requireActive();
        var game = scene.game().snapshot();
        int label = 0xF5EDE5FF, value = 0xFFFFFFFF, accent = 0x4AE0FFFF;
        return new GamePresentation(GamePresentation.ViewMode.TWO_D, List.of(
                new GamePresentation.Label(.055, .18, "LINES", label),
                new GamePresentation.Label(.055, .22, Integer.toString(game.lines()), accent),
                new GamePresentation.Label(.73, .18, "TIME", label),
                new GamePresentation.Label(.73, .22, String.format(Locale.ROOT, "%02d:%02d",
                        (int) game.elapsedSeconds() / 60, (int) game.elapsedSeconds() % 60), value),
                new GamePresentation.Label(.73, .31, "SCORE", label),
                new GamePresentation.Label(.73, .35, Integer.toString(game.score()), value),
                new GamePresentation.Label(.73, .44, "TOTAL SCORE", label),
                new GamePresentation.Label(.73, .48, Integer.toString(game.totalScore()), accent)),
                "Arrows move/rotate | Space hard drop | R restart");
    }

    @Override public void close() {
        closed = true;
        scene = null;
        context = null;
    }

    private void requireActive() {
        if (!initialized || closed)
            throw new IllegalStateException("Module is not active");
    }
}
