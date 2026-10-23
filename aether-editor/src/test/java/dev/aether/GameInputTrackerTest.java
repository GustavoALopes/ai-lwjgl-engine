package dev.aether;

import dev.aether.editor.GameInputTracker;
import dev.aether.game.GameKey;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameInputTrackerTest {
    @Test void reportsOnlyRisingEdgesAndAcceptsReleaseAndRepress() {
        var tracker = new GameInputTracker();
        assertEquals(Set.of(GameKey.LEFT), tracker.poll(Set.of(GameKey.LEFT)).pressed());
        assertTrue(tracker.poll(Set.of(GameKey.LEFT)).pressed().isEmpty());
        tracker.poll(Set.of());
        assertEquals(Set.of(GameKey.LEFT), tracker.poll(Set.of(GameKey.LEFT)).pressed());
    }

    @Test void captureSuppressesHeldKeysUntilReleaseAndResetForgetsPreviousSession() {
        var tracker = new GameInputTracker();
        tracker.suppress(Set.of(GameKey.R));
        var captured = tracker.poll(Set.of(GameKey.R));
        assertTrue(captured.pressed().isEmpty());
        assertTrue(captured.held().isEmpty());
        tracker.poll(Set.of());
        assertEquals(Set.of(GameKey.R), tracker.poll(Set.of(GameKey.R)).pressed());
        tracker.reset();
        assertEquals(Set.of(GameKey.R), tracker.poll(Set.of(GameKey.R)).pressed());
    }
}
