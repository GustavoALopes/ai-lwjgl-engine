package dev.aether.editor;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/** Clear the window back buffer before composing the editor's panels and transient overlays. */
final class EditorFramebuffer {
    private EditorFramebuffer() { }

    static void clear(int width, int height) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glDisable(GL_SCISSOR_TEST);
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glStencilMask(0xff);
        glClearColor(.08f, .09f, .11f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }
}
