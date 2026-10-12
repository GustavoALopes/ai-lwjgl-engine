package dev.aether.render;

import dev.aether.platform.LwjglWindow;
import static org.lwjgl.opengl.GL11.*;

/** First OpenGL presentation pass. Later PBR passes retain this lifecycle boundary. */
public final class OpenGlRenderer {
    private final RenderSettings settings;
    public OpenGlRenderer(RenderSettings settings) { this.settings = settings; }
    public void render(LwjglWindow window) {
        int[] width = new int[1], height = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(window.handle(), width, height);
        glViewport(0, 0, width[0], height[0]);
        glClearColor(0.04f, 0.06f, 0.10f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    public RenderSettings settings() { return settings; }
}
