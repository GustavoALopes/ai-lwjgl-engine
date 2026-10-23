package dev.aether.editor;

import dev.aether.game.GameInput;
import dev.aether.game.GameKey;
import java.util.HashSet;
import java.util.Set;

/** Converts platform polling to immutable rising-edge snapshots without native dependencies. */
public final class GameInputTracker {
    private Set<GameKey> previous = Set.of();
    private Set<GameKey> suppressed = new HashSet<>();

    public GameInput poll(Set<GameKey> held) {
        Set<GameKey> raw = Set.copyOf(held);
        suppressed.retainAll(raw);
        Set<GameKey> eligible = new HashSet<>(raw);
        eligible.removeAll(suppressed);
        Set<GameKey> snapshot = Set.copyOf(eligible);
        Set<GameKey> pressed = new HashSet<>(snapshot);
        pressed.removeAll(previous);
        previous = snapshot;
        return new GameInput(snapshot, pressed);
    }

    /** Suppresses held keys until release when focus/text capture changes. */
    public void suppress(Set<GameKey> held) {
        suppressed = new HashSet<>(Set.copyOf(held));
        previous = Set.of();
    }

    public void reset() { previous = Set.of(); suppressed.clear(); }
}
