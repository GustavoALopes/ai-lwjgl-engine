package dev.aether.demo;

import dev.aether.ecs.World;
import dev.aether.ecs.WorldSystem;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Deterministic, headless Tetris rules suitable for registration in an Aether
 * {@link World}. Register this system before any presentation system:
 * {@code world.addSystem(tetris)}. Input adapters call the command methods.
 */
public final class TetrisDemo implements WorldSystem {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 20;
    private static final double FALL_INTERVAL_SECONDS = 0.75;

    private final int[][] board = new int[HEIGHT][WIDTH];
    private final Tetromino[] scriptedPieces;
    private final Random random;
    private int scriptedIndex;
    private Piece active;
    private double fallTime;
    private int score;
    private int totalScore;
    private int lines;
    private double elapsedSeconds;
    private double clearPulse;
    private double lockPulse;
    private boolean gameOver;

    /** Creates a repeatable game whose piece sequence is derived from {@code seed}. */
    public TetrisDemo(long seed) {
        scriptedPieces = null;
        random = new Random(seed);
        spawn();
    }

    /** Creates a repeatable game which cycles through the supplied pieces. */
    public TetrisDemo(Tetromino... pieces) {
        Objects.requireNonNull(pieces, "pieces");
        if (pieces.length == 0) throw new IllegalArgumentException("At least one piece is required");
        scriptedPieces = pieces.clone();
        for (Tetromino piece : scriptedPieces) Objects.requireNonNull(piece, "pieces must not contain null");
        random = null;
        spawn();
    }

    /** Advances gravity. The world parameter is intentionally unused: game state is headless. */
    @Override public void update(World world, double deltaSeconds) { tick(deltaSeconds); }

    public void tick(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0) throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        if (gameOver) return;
        elapsedSeconds += deltaSeconds;
        clearPulse = Math.max(0, clearPulse - deltaSeconds * 1.8);
        lockPulse = Math.max(0, lockPulse - deltaSeconds * 4);
        fallTime += deltaSeconds;
        while (fallTime >= FALL_INTERVAL_SECONDS && !gameOver) {
            fallTime -= FALL_INTERVAL_SECONDS;
            stepDown();
        }
    }

    public boolean moveLeft() { return move(-1, 0); }
    public boolean moveRight() { return move(1, 0); }
    public boolean rotateClockwise() {
        if (gameOver) return false;
        Piece rotated = active.rotate();
        if (!fits(rotated)) return false;
        active = rotated;
        return true;
    }
    /** Moves down once, locking the piece when it can no longer fall. */
    public boolean softDrop() { return stepDown(); }
    /** Drops immediately and returns the number of rows travelled. */
    public int hardDrop() {
        int distance = 0;
        while (stepDown()) distance++;
        return distance;
    }

    /** Starts a clean board while preserving this demo's configured piece source. */
    public void reset() {
        for (int[] row : board) Arrays.fill(row, 0);
        scriptedIndex = 0;
        active = null;
        fallTime = 0;
        score = 0;
        totalScore = 0;
        lines = 0;
        elapsedSeconds = 0;
        clearPulse = 0;
        lockPulse = 0;
        gameOver = false;
        spawn();
    }

    public Snapshot snapshot() {
        int[][] locked = Arrays.stream(board).map(int[]::clone).toArray(int[][]::new);
        List<Cell> activeCells = active == null ? List.of() : Arrays.stream(active.cells())
                .map(cell -> new Cell(cell[0], cell[1], active.type())).toList();
        return new Snapshot(locked, activeCells, score, totalScore, lines, elapsedSeconds, clearPulse, lockPulse, gameOver);
    }

    private boolean move(int dx, int dy) {
        if (gameOver) return false;
        Piece moved = active.move(dx, dy);
        if (!fits(moved)) return false;
        active = moved;
        return true;
    }
    private boolean stepDown() {
        if (move(0, 1)) return true;
        lock();
        return false;
    }
    private void lock() {
        lockPulse = 1;
        for (int[] cell : active.cells()) if (cell[1] >= 0) board[cell[1]][cell[0]] = active.type().ordinal() + 1;
        int cleared = clearLines();
        lines += cleared;
        int earned = switch (cleared) { case 1 -> 100; case 2 -> 300; case 3 -> 500; case 4 -> 800; default -> 0; };
        score += earned;
        totalScore += earned;
        if (cleared > 0) clearPulse = Math.min(1, cleared / 4d);
        spawn();
    }
    private int clearLines() {
        int write = HEIGHT - 1;
        int cleared = 0;
        for (int read = HEIGHT - 1; read >= 0; read--) {
            if (isFull(board[read])) { cleared++; continue; }
            if (write != read) board[write] = board[read].clone();
            write--;
        }
        while (write >= 0) board[write--] = new int[WIDTH];
        return cleared;
    }
    private static boolean isFull(int[] row) { for (int value : row) if (value == 0) return false; return true; }
    private void spawn() {
        Tetromino type = scriptedPieces == null ? Tetromino.values()[random.nextInt(Tetromino.values().length)] : scriptedPieces[scriptedIndex++ % scriptedPieces.length];
        active = new Piece(type, 3, 0, 0);
        if (!fits(active)) gameOver = true;
    }
    private boolean fits(Piece piece) {
        for (int[] cell : piece.cells()) {
            if (cell[0] < 0 || cell[0] >= WIDTH || cell[1] >= HEIGHT) return false;
            if (cell[1] >= 0 && board[cell[1]][cell[0]] != 0) return false;
        }
        return true;
    }

    public enum Tetromino { I, O, T, S, Z, J, L }
    public record Cell(int x, int y, Tetromino type) { public Cell { Objects.requireNonNull(type, "type"); } }
    /** Immutable view; {@link #lockedCells()} returns a defensive deep copy. */
    public record Snapshot(int[][] lockedCells, List<Cell> activeCells, int score, int totalScore, int lines, double elapsedSeconds, double clearPulse, double lockPulse, boolean gameOver) {
        public Snapshot { lockedCells = Arrays.stream(lockedCells).map(int[]::clone).toArray(int[][]::new); activeCells = List.copyOf(activeCells); }
        @Override public int[][] lockedCells() { return Arrays.stream(lockedCells).map(int[]::clone).toArray(int[][]::new); }
    }

    private record Piece(Tetromino type, int x, int y, int rotation) {
        Piece move(int dx, int dy) { return new Piece(type, x + dx, y + dy, rotation); }
        Piece rotate() { return type == Tetromino.O ? this : new Piece(type, x, y, (rotation + 1) % 4); }
        int[][] cells() {
            int[][] base = switch (type) {
                case I -> new int[][]{{0, 1}, {1, 1}, {2, 1}, {3, 1}};
                case O -> new int[][]{{1, 0}, {2, 0}, {1, 1}, {2, 1}};
                case T -> new int[][]{{1, 0}, {0, 1}, {1, 1}, {2, 1}};
                case S -> new int[][]{{1, 0}, {2, 0}, {0, 1}, {1, 1}};
                case Z -> new int[][]{{0, 0}, {1, 0}, {1, 1}, {2, 1}};
                case J -> new int[][]{{0, 0}, {0, 1}, {1, 1}, {2, 1}};
                case L -> new int[][]{{2, 0}, {0, 1}, {1, 1}, {2, 1}};
            };
            int dimension = type == Tetromino.I ? 4 : 3;
            int[][] result = new int[base.length][2];
            for (int index = 0; index < base.length; index++) {
                int px = base[index][0], py = base[index][1];
                for (int turn = 0; turn < rotation; turn++) { int nextX = dimension - 1 - py; py = px; px = nextX; }
                result[index][0] = x + px;
                result[index][1] = y + py;
            }
            return result;
        }
    }
}
