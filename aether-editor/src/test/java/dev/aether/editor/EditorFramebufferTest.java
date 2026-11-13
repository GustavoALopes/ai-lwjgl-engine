package dev.aether.editor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.lwjgl.opengl.GL;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

@EnabledIfSystemProperty(named = "aether.test.opengl", matches = "true")
class EditorFramebufferTest {
    @Test void removesTransientOverlaysOnEveryFrameEvenWhenPreviousPassLeftMasks() {
        assertTrue(glfwInit());
        long window = 0;
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(64, 64, "Editor background test", NULL, NULL);
            assertNotEquals(NULL, window);
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            for (int frame = 0; frame < 4; frame++) {
                glDisable(GL_SCISSOR_TEST);
                glClearColor(1, 0, 0, 1);
                glClear(GL_COLOR_BUFFER_BIT);
                glEnable(GL_SCISSOR_TEST);
                glScissor(0, 0, 1, 1);
                glColorMask(false, false, false, false);
                glDepthMask(false);
                glStencilMask(0);
                EditorFramebuffer.clear(64, 64);
                var pixels = memAlloc(64 * 64 * 4);
                try {
                    glReadPixels(0, 0, 64, 64, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                    for (int offset = 0; offset < pixels.capacity(); offset += 4) {
                        assertEquals(20, Byte.toUnsignedInt(pixels.get(offset)), 1);
                        assertEquals(23, Byte.toUnsignedInt(pixels.get(offset + 1)), 1);
                        assertEquals(28, Byte.toUnsignedInt(pixels.get(offset + 2)), 1);
                    }
                } finally { memFree(pixels); }
                assertEquals(GL_NO_ERROR, glGetError());
                glfwSwapBuffers(window);
            }
        } finally {
            GL.setCapabilities(null);
            if (window != 0) glfwDestroyWindow(window);
            glfwTerminate();
        }
    }
}
