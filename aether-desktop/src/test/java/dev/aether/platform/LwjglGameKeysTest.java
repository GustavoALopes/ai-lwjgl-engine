package dev.aether.platform;

import dev.aether.game.GameKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class LwjglGameKeysTest {
    @Test void mapsLettersAndSpecialKeysWithoutANativeContext() {
        assertEquals(GLFW_KEY_LEFT, LwjglGameKeys.code(GameKey.LEFT));
        assertEquals(GLFW_KEY_SPACE, LwjglGameKeys.code(GameKey.SPACE));
        assertEquals(GLFW_KEY_LEFT_CONTROL, LwjglGameKeys.code(GameKey.CONTROL));
        for (GameKey key : GameKey.values()) {
            if (key.name().length() == 1)
                assertEquals(key.name().charAt(0), LwjglGameKeys.code(key));
            assertTrue(LwjglGameKeys.code(key) >= 0);
        }
        assertThrows(NullPointerException.class, () -> LwjglGameKeys.code(null));
    }
}
