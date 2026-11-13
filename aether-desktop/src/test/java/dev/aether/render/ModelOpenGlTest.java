package dev.aether.render;

import dev.aether.ecs.World;
import dev.aether.scene.Transform;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.opengl.GL;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Opt-in native rendering and GPU ownership check. */
@EnabledIfSystemProperty(named = "aether.test.opengl", matches = "true")
class ModelOpenGlTest {
    @TempDir Path directory;

    @Test void rendersImportedMaterialAndReleasesBuffersOnDeleteSwitchAndClose() throws Exception {
        Files.writeString(directory.resolve("red.mtl"), "newmtl red\nKd 1 0 0\n");
        Path file = directory.resolve("triangle.obj");
        Files.writeString(file, "mtllib red.mtl\nv -1 -1 0\nv 1 -1 0\nv 0 1 0\nusemtl red\nf 1 2 3\n");
        ModelMesh mesh = ObjModelLoader.load(file);
        assertTrue(glfwInit());
        long window = 0;
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            window = glfwCreateWindow(256, 256, "Model test", NULL, NULL);
            assertNotEquals(NULL, window);
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            int finalBuffer;
            try (var world = new World(); var other = new World();
                 var renderer = new OpenGlRenderer(RenderSettings.productionDefaults())) {
                var entity = world.create("model");
                world.add(entity, new MeshRenderer("triangle.obj", "imported"));
                world.add(entity, Transform.identity());
                world.add(entity, mesh);
                var projection = new Matrix4f().perspective((float) Math.toRadians(55), 1, .1f, 100);
                var view = new Matrix4f().lookAt(0, 0, 3, 0, 0, 0, 0, 1, 0);
                int texture = renderer.renderScene(world, 256, 256, projection, view);
                int buffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
                assertTrue(glIsBuffer(buffer));
                var pixels = memAlloc(256 * 256 * 4);
                try {
                    glBindTexture(GL_TEXTURE_2D, texture);
                    glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                    int center = (128 * 256 + 128) * 4;
                    assertTrue(Byte.toUnsignedInt(pixels.get(center)) > 100, "Imported triangle must be visible");
                    assertEquals(0, Byte.toUnsignedInt(pixels.get(center + 1)), 2);
                    assertEquals(0, Byte.toUnsignedInt(pixels.get(center + 2)), 2);
                } finally { memFree(pixels); }
                world.destroy(entity);
                renderer.renderScene(world, 256, 256, projection, view);
                assertFalse(glIsBuffer(buffer), "Deleting the last instance must release its buffer");
                entity = world.create("again");
                world.add(entity, new MeshRenderer("triangle.obj", "imported"));
                world.add(entity, Transform.identity());
                world.add(entity, mesh);
                renderer.renderScene(world, 256, 256, projection, view);
                buffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
                renderer.renderScene(other, 256, 256, projection, view);
                assertFalse(glIsBuffer(buffer), "Switching worlds must release old geometry");
                renderer.renderScene(world, 256, 256, projection, view);
                buffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
                renderer.releaseImportedModels();
                assertFalse(glIsBuffer(buffer), "Closing a project must release geometry");
                renderer.renderScene(world, 256, 256, projection, view);
                finalBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
                assertEquals(GL_NO_ERROR, glGetError());
            }
            assertFalse(glIsBuffer(finalBuffer), "Renderer close must release geometry");
            assertEquals(GL_NO_ERROR, glGetError());
        } finally {
            GL.setCapabilities(null);
            if (window != 0) glfwDestroyWindow(window);
            glfwTerminate();
        }
    }
}
