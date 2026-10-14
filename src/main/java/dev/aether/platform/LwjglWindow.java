package dev.aether.platform;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Owns one GLFW window and its OpenGL context. This class must be used on its creating thread. */
public final class LwjglWindow implements AutoCloseable {
    private final long handle;
    private boolean closed;

    public LwjglWindow(WindowConfig config) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, config.resizable() ? GLFW_TRUE : GLFW_FALSE);
        handle = glfwCreateWindow(config.width(), config.height(), config.title(), NULL, NULL);
        if (handle == NULL) { glfwTerminate(); throw new IllegalStateException("Unable to create GLFW window"); }
        glfwMakeContextCurrent(handle);
        glfwSwapInterval(config.vSync() ? 1 : 0);
        GL.createCapabilities();
        glfwShowWindow(handle);
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public void pollEvents() { glfwPollEvents(); }
    public void present() { glfwSwapBuffers(handle); }
    public void requestClose() { glfwSetWindowShouldClose(handle, true); }
    public long handle() { return handle; }

    @Override public void close() {
        if (closed) return;
        closed = true;
        glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) callback.free();
    }
}
