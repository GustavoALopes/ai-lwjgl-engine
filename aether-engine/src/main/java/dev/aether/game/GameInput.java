package dev.aether.game;

import java.util.Objects;
import java.util.Set;
import java.util.EnumSet;
import java.util.Collections;

/** Immutable keyboard snapshot; pressed contains the rising edges within held. */
public record GameInput(Set<GameKey> held, Set<GameKey> pressed) {
    public GameInput {
        held = snapshot(held, "held");
        pressed = snapshot(pressed, "pressed");
        if (!held.containsAll(pressed))
            throw new IllegalArgumentException("Pressed keys must also be held");
    }

    public static GameInput empty() { return new GameInput(Set.of(), Set.of()); }

    private static Set<GameKey> snapshot(Set<GameKey> keys, String name) {
        var ordered = EnumSet.noneOf(GameKey.class);
        ordered.addAll(Objects.requireNonNull(keys, name));
        return Collections.unmodifiableSet(ordered);
    }
}
