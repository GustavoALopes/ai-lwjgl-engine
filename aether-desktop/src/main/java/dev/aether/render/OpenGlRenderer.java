package dev.aether.render;

import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import java.nio.FloatBuffer;
import java.util.Objects;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * OpenGL adapter. Feature flags are consumed here, never by the headless ECS.
 */
public final class OpenGlRenderer implements AutoCloseable {
    private static final float[] CUBE = {
            -1, -1, 1, 0, 0, 1, -1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, 1, 1, 0, 1, -1, -1, 1, 0, 0,
            -1, -1, -1, 1, 0, -1, 1, -1, 1, 1, 1, 1, -1, 0, 1, 1, 1, -1, 0, 1, 1, -1, -1, 0, 0, -1, -1, -1, 1, 0,
            -1, 1, -1, 0, 1, -1, 1, 1, 1, 1, -1, -1, 1, 1, 0, -1, -1, 1, 1, 0, -1, -1, -1, 0, 0, -1, 1, -1, 0, 1,
            1, 1, 1, 0, 1, 1, 1, -1, 1, 1, 1, -1, -1, 1, 0, 1, -1, -1, 1, 0, 1, -1, 1, 0, 0, 1, 1, 1, 0, 1,
            -1, -1, -1, 0, 0, 1, -1, -1, 1, 0, 1, -1, 1, 1, 1, 1, -1, 1, 1, 1, -1, -1, 1, 0, 1, -1, -1, -1, 0, 0,
            -1, 1, -1, 0, 0, -1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, 1, 0, -1, 1, -1, 0, 0
    };
    private final RenderSettings settings;
    private int vao, vbo, program, post, framebuffer, displayFramebuffer, sceneTexture, displayTexture, depth, width,
            height;
    private Texture2D checker;

    public OpenGlRenderer(RenderSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public RenderSettings settings() {
        return settings;
    }

    /** Requires an active render context and returns the frame texture. */
    public int renderScene(World world, int requestedWidth, int requestedHeight, Matrix4f projection, Matrix4f view) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(view, "view");
        if (requestedWidth < 1 || requestedHeight < 1)
            return displayTexture;
        if (program == 0)
            init();
        resize(requestedWidth, requestedHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
        glEnable(GL_DEPTH_TEST);
        glClearColor(.035f, .055f, .09f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(program);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, checker.id());
        glUniform1i(glGetUniformLocation(program, "tex"), 0);
        glUniform1i(glGetUniformLocation(program, "ssao"), settings.ssao() ? 1 : 0);
        glUniform1i(glGetUniformLocation(program, "shadows"), settings.softShadows() ? 1 : 0);
        glUniform1i(glGetUniformLocation(program, "ibl"), settings.imageBasedLighting() ? 1 : 0);
        glBindVertexArray(vao);
        for (var entry : world.view(MeshRenderer.class).entrySet()) {
            Transform transform = world.get(entry.getKey(), Transform.class).orElse(null);
            if (transform == null)
                continue;
            Matrix4f model = model(transform);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer b = stack.mallocFloat(16);
                new Matrix4f(projection).mul(view).mul(model).get(b);
                glUniformMatrix4fv(glGetUniformLocation(program, "mvp"), false, b);
                model.get(b);
                glUniformMatrix4fv(glGetUniformLocation(program, "model"), false, b);
            }
            Material material = world.get(entry.getKey(), Material.class)
                    .orElseGet(() -> new Material(colorFor(entry.getValue().materialId()), 0));
            Vec3 color = material.color();
            glUniform3f(glGetUniformLocation(program, "tint"), (float) color.x(), (float) color.y(), (float) color.z());
            glUniform1f(glGetUniformLocation(program, "emissive"),
                    (float) material.emissive());
            glDrawArrays(GL_TRIANGLES, 0, 36);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, displayFramebuffer);
        glViewport(0, 0, width, height);
        glDisable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(post);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        glUniform1i(glGetUniformLocation(post, "scene"), 0);
        glUniform1f(glGetUniformLocation(post, "exposure"), (float) settings.exposure());
        glUniform1i(glGetUniformLocation(post, "hdr"), settings.hdr() ? 1 : 0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return displayTexture;
    }

    private Matrix4f model(Transform t) {
        return new Matrix4f().translate((float) t.position().x(), (float) t.position().y(), (float) t.position().z())
                .rotateXYZ((float) Math.toRadians(t.rotationDegrees().x()),
                        (float) Math.toRadians(t.rotationDegrees().y()),
                        (float) Math.toRadians(t.rotationDegrees().z()))
                .scale((float) t.scale().x(), (float) t.scale().y(), (float) t.scale().z());
    }

    private Vec3 colorFor(String material) {
        return switch (material) {
            case "pbr/metallic-blue" -> new Vec3(.08, .35, .95);
            case "pbr/emissive-orange" -> new Vec3(1, .25, .04);
            case "pbr/checker" -> new Vec3(.2, .27, .34);
            default -> new Vec3(.75, .75, .75);
        };
    }

    private void init() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, CUBE, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
        program = link(ShaderSource.load("/shaders/engine/engine.vert"),
                ShaderSource.load("/shaders/engine/engine.frag"));
        post = link(ShaderSource.load("/shaders/engine/post.vert"),
                ShaderSource.load("/shaders/engine/post.frag"));
        framebuffer = glGenFramebuffers();
        displayFramebuffer = glGenFramebuffers();
        sceneTexture = glGenTextures();
        displayTexture = glGenTextures();
        depth = glGenRenderbuffers();
        checker = Texture2D.checkerboard();
    }

    private void resize(int w, int h) {
        if (width == w && height == h)
            return;
        width = w;
        height = h;
        for (int t : new int[] { sceneTexture, displayTexture }) {
            glBindTexture(GL_TEXTURE_2D, t);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }
        glBindRenderbuffer(GL_RENDERBUFFER, depth);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sceneTexture, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
        checkFramebuffer("Scene");
        glBindFramebuffer(GL_FRAMEBUFFER, displayFramebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, displayTexture, 0);
        checkFramebuffer("Display");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void checkFramebuffer(String name) {
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException(name + " framebuffer is incomplete");
    }

    private int link(String vs, String fs) {
        int v = compile(GL_VERTEX_SHADER, vs), f = compile(GL_FRAGMENT_SHADER, fs), p = glCreateProgram();
        glAttachShader(p, v);
        glAttachShader(p, f);
        glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE)
            throw new IllegalStateException(glGetProgramInfoLog(p));
        glDeleteShader(v);
        glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new IllegalStateException(glGetShaderInfoLog(shader));
        return shader;
    }

    @Override
    public void close() {
        if (checker != null)
            checker.close();
        if (depth != 0)
            glDeleteRenderbuffers(depth);
        if (sceneTexture != 0)
            glDeleteTextures(sceneTexture);
        if (displayTexture != 0)
            glDeleteTextures(displayTexture);
        if (framebuffer != 0)
            glDeleteFramebuffers(framebuffer);
        if (displayFramebuffer != 0)
            glDeleteFramebuffers(displayFramebuffer);
        if (vbo != 0)
            glDeleteBuffers(vbo);
        if (vao != 0)
            glDeleteVertexArrays(vao);
        if (program != 0)
            glDeleteProgram(program);
        if (post != 0)
            glDeleteProgram(post);
    }
}
