package dev.aether;

import dev.aether.demo.TetrisDemo;
import dev.aether.demo.TetrisDemo.Tetromino;
import dev.aether.demo.TetrisScene;
import dev.aether.ecs.World;
import dev.aether.render.MeshRenderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TetrisDemoTest {
    @Test void gravityMovesThenLocksAPiece() {
        TetrisDemo game = new TetrisDemo(Tetromino.O);
        game.tick(.75);
        assertTrue(game.snapshot().activeCells().stream().allMatch(cell -> cell.y() >= 1));
        game.hardDrop();
        assertEquals(4, occupied(game.snapshot().lockedCells()));
        assertFalse(game.snapshot().gameOver());
    }

    @Test void fiveSquarePiecesClearTwoLinesAndAwardScore() {
        TetrisDemo game = new TetrisDemo(Tetromino.O);
        for (int position : new int[]{-1, 1, 3, 5, 7}) {
            while (game.snapshot().activeCells().getFirst().x() > position + 1) assertTrue(game.moveLeft());
            while (game.snapshot().activeCells().getFirst().x() < position + 1) assertTrue(game.moveRight());
            game.hardDrop();
        }
        assertEquals(0, occupied(game.snapshot().lockedCells()));
        assertEquals(2, game.snapshot().lines());
        assertEquals(300, game.snapshot().score());
        assertEquals(300, game.snapshot().totalScore());
        assertEquals(.5, game.snapshot().clearPulse(), 1e-9);
        game.tick(1);
        assertEquals(1, game.snapshot().elapsedSeconds(), 1e-9);
        assertEquals(0, game.snapshot().clearPulse(), 1e-9);
    }

    @Test void snapshotsDoNotExposeBoardStateAndTimeMustBeValid() {
        TetrisDemo game = new TetrisDemo(Tetromino.T);
        int[][] copy = game.snapshot().lockedCells();
        copy[0][0] = 99;
        assertEquals(0, game.snapshot().lockedCells()[0][0]);
        assertThrows(IllegalArgumentException.class, () -> game.tick(-.01));
    }

    @Test void sceneOpensAsAHeadlessEcsBoardAndCanBeReset() {
        World world = new World();
        TetrisScene scene = new TetrisScene(new TetrisDemo(Tetromino.I));
        scene.open(world);
        assertEquals(201, world.entityCount());
        assertEquals(200, world.view(MeshRenderer.class).size());
        assertEquals(4, world.view(MeshRenderer.class).values().stream().filter(mesh -> mesh.materialId().equals("tetris/cyan")).count());
        scene.game().hardDrop();
        scene.open(world);
        assertEquals(201, world.entityCount());
        assertEquals(0, occupied(scene.game().snapshot().lockedCells()));
    }

    private static int occupied(int[][] board) { int total = 0; for (int[] row : board) for (int cell : row) if (cell != 0) total++; return total; }
}
