package dev.aether.render;

import dev.aether.ecs.World;
import dev.aether.game.GamePresentation;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.lwjgl.opengl.GL;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Opt-in adapter test: mvn test -Daether.test.opengl=true. Normal tests remain headless. */
@EnabledIfSystemProperty(named = "aether.test.opengl", matches = "true")
class HudOpenGlTest {
    @Test void rendersStencilBackedHudAfterPostProcessingAndSurvivesResize() throws Exception {
        assertTrue(glfwInit());
        long window = 0;
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(920, 600, "HUD test", NULL, NULL);
            assertNotEquals(NULL, window);
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            var presentation = new GamePresentation(GamePresentation.ViewMode.THREE_D, List.of(), "", List.of(
                    new GamePresentation.Stat(GamePresentation.Anchor.TOP_LEFT, "STAGE", "01"),
                    new GamePresentation.Stat(GamePresentation.Anchor.TOP_CENTER, "SCORE", "1,250")));
            // Lower scene exposure also proves that HUD colors bypass tone mapping.
            try (var world = new World(); var renderer = new OpenGlRenderer(new RenderSettings(true, true, true, true, .25))) {
                for (int width : new int[] { 920, 640, 920 }) {
                    int texture = renderer.renderScene(world, width, 600, new Matrix4f(), new Matrix4f(), presentation, 1);
                    var pixels = memAlloc(width * 600 * 4);
                    try {
                        glBindTexture(GL_TEXTURE_2D, texture);
                        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                        int corner = ((600 - 20) * width + 20) * 4;
                        assertEquals(54, Byte.toUnsignedInt(pixels.get(corner)), 1);
                        assertEquals(69, Byte.toUnsignedInt(pixels.get(corner + 1)), 1);
                        int stageGlyphs = 0, scoreGlyphs = 0;
                        var image = new BufferedImage(width, 600, BufferedImage.TYPE_INT_ARGB);
                        for (int y = 0; y < 600; y++) for (int x = 0; x < width; x++) {
                            int offset = ((599 - y) * width + x) * 4;
                            int r = Byte.toUnsignedInt(pixels.get(offset));
                            int g = Byte.toUnsignedInt(pixels.get(offset + 1));
                            int b = Byte.toUnsignedInt(pixels.get(offset + 2));
                            image.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | b);
                            if (y < 108 && r > 230 && g > 230 && b > 230) {
                                if (x < 100) stageGlyphs++;
                                else scoreGlyphs++;
                            }
                        }
                        assertTrue(stageGlyphs > 100, "Stage must render visible glyphs");
                        assertTrue(scoreGlyphs > 100, "Score must render visible glyphs");
                        ImageIO.write(image, "png", Path.of("target", "hud-preview.png").toFile());
                    } finally { memFree(pixels); }
                    assertEquals(GL_NO_ERROR, glGetError());
                    renderer.renderScene(world, width, 600, new Matrix4f(), new Matrix4f());
                    assertEquals(GL_NO_ERROR, glGetError());
                }
                int texture = renderer.renderScene(world, 1840, 1200, new Matrix4f(), new Matrix4f(), presentation, 2);
                var pixels = memAlloc(1840 * 1200 * 4);
                try {
                    glBindTexture(GL_TEXTURE_2D, texture);
                    glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                    assertEquals(54, Byte.toUnsignedInt(pixels.get(((1200 - 40) * 1840 + 40) * 4)), 1);
                    assertEquals(54, Byte.toUnsignedInt(pixels.get(((1200 - 40) * 1840 + 920) * 4)), 1);
                    assertEquals(GL_NO_ERROR, glGetError());
                } finally { memFree(pixels); }
                renderer.close();
                assertThrows(IllegalStateException.class, () -> renderer.renderScene(world, 920, 600,
                        new Matrix4f(), new Matrix4f(), presentation, 1));
            }
            assertEquals(GL_NO_ERROR, glGetError());
        } finally {
            GL.setCapabilities(null);
            if (window != NULL) glfwDestroyWindow(window);
            glfwTerminate();
        }
    }
}
