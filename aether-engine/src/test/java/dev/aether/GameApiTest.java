package dev.aether;

import dev.aether.game.*;
import dev.aether.math.Vec3;
import dev.aether.render.Material;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameApiTest {
    @Test void inputSnapshotsAreDefensiveAndRejectImpossiblePressedStates() {
        var keys = new HashSet<>(Set.of(GameKey.LEFT));
        var input = new GameInput(keys, keys);
        keys.clear();
        assertEquals(Set.of(GameKey.LEFT), input.held());
        assertThrows(UnsupportedOperationException.class, () -> input.pressed().clear());
        assertThrows(IllegalArgumentException.class, () -> new GameInput(Set.of(), Set.of(GameKey.SPACE)));
        assertTrue(GameInput.empty().pressed().isEmpty());
        assertEquals(java.util.List.of(GameKey.LEFT, GameKey.A, GameKey.Z),
                new ArrayList<>(new GameInput(Set.of(GameKey.Z, GameKey.A, GameKey.LEFT), Set.of()).held()));
    }

    @Test void hudSnapshotsAreImmutableAndValidateNormalizedCoordinates() {
        var labels = new ArrayList<GamePresentation.Label>();
        labels.add(new GamePresentation.Label(.5, .5, "Score", 0xFFFFFFFF));
        var presentation = new GamePresentation(GamePresentation.ViewMode.TWO_D, labels, "Space");
        labels.clear();
        assertEquals(1, presentation.labels().size());
        assertThrows(UnsupportedOperationException.class, () -> presentation.labels().clear());
        assertThrows(IllegalArgumentException.class,
                () -> new GamePresentation.Label(Double.NaN, 0, "bad", 0));
        assertThrows(IllegalArgumentException.class, () -> new GamePresentation.Label(1.1, 0, "bad", 0));
        assertThrows(NullPointerException.class, () -> new GamePresentation.Label(0, 0, null, 0));
    }

    @Test void materialsSupportHdrAndRejectInvalidValues() {
        assertEquals(6, new Material(new Vec3(1.4, .2, .1), 6).emissive());
        assertThrows(IllegalArgumentException.class, () -> new Material(new Vec3(-1, 0, 0), 0));
        assertThrows(IllegalArgumentException.class, () -> new Material(new Vec3(0, Double.NaN, 0), 0));
        assertThrows(IllegalArgumentException.class, () -> new Material(Vec3.ZERO, Double.POSITIVE_INFINITY));
    }
}
