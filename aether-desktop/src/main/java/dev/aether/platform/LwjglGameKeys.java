package dev.aether.platform;

import dev.aether.game.GameKey;
import java.util.Objects;
import static org.lwjgl.glfw.GLFW.*;

/** Converts the engine's keyboard vocabulary to GLFW codes without creating a context. */
public final class LwjglGameKeys {
    private LwjglGameKeys() { }
    public static int code(GameKey key) {
        return switch (Objects.requireNonNull(key, "key")) {
            case LEFT -> GLFW_KEY_LEFT;
            case RIGHT -> GLFW_KEY_RIGHT;
            case UP -> GLFW_KEY_UP;
            case DOWN -> GLFW_KEY_DOWN;
            case SPACE -> GLFW_KEY_SPACE;
            case ENTER -> GLFW_KEY_ENTER;
            case ESCAPE -> GLFW_KEY_ESCAPE;
            case TAB -> GLFW_KEY_TAB;
            case SHIFT -> GLFW_KEY_LEFT_SHIFT;
            case CONTROL -> GLFW_KEY_LEFT_CONTROL;
            default -> key.name().charAt(0); // A-Z use their ASCII codes in GLFW.
        };
    }
}
