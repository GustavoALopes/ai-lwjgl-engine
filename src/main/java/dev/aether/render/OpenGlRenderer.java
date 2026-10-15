package dev.aether.render;

import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.nio.FloatBuffer;
import java.util.Objects;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** OpenGL scene pass rendered to a texture that the editor embeds in its Scene View. */
public final class OpenGlRenderer implements AutoCloseable {
    /** Position (xyz) and explicit face-local UV coordinates (uv), six vertices per cube face. */
    private static final float[] CUBE_VERTICES = {
        -1,-1, 1,0,0, 1,-1, 1,1,0, 1, 1, 1,1,1, 1, 1, 1,1,1,-1, 1, 1,0,1,-1,-1, 1,0,0,
        -1,-1,-1,1,0,-1, 1,-1,1,1, 1, 1,-1,0,1, 1, 1,-1,0,1, 1,-1,-1,0,0,-1,-1,-1,1,0,
        -1, 1,-1,0,1,-1, 1, 1,1,1,-1,-1, 1,1,0,-1,-1, 1,1,0,-1,-1,-1,0,0,-1, 1,-1,0,1,
         1, 1, 1,0,1, 1, 1,-1,1,1, 1,-1,-1,1,0, 1,-1,-1,1,0, 1,-1, 1,0,0, 1, 1, 1,0,1,
        -1,-1,-1,0,0, 1,-1,-1,1,0, 1,-1, 1,1,1, 1,-1, 1,1,1,-1,-1, 1,0,1,-1,-1,-1,0,0,
        -1, 1,-1,0,0,-1, 1, 1,0,1, 1, 1, 1,1,1, 1, 1, 1,1,1, 1, 1,-1,1,0,-1, 1,-1,0,0
    };
    private static final String VERTEX="#version 410 core\nlayout(location=0) in vec3 position; layout(location=1) in vec2 texCoord; uniform mat4 uMvp; out vec2 vUv; void main(){vUv=texCoord;gl_Position=uMvp*vec4(position,1.0);}";
    private static final String FRAGMENT="#version 410 core\nin vec2 vUv; out vec4 color; uniform sampler2D uAlbedo; uniform vec3 uTint; void main(){color=vec4(texture(uAlbedo,vUv).rgb*uTint,1.0);}";
    private final RenderSettings settings; private int vao,vbo,program,framebuffer,displayFramebuffer,sceneTexture,displayTexture,depthBuffer,width,height; private Texture2D checkerTexture;
    public OpenGlRenderer(RenderSettings settings){this.settings=settings;} public RenderSettings settings(){return settings;}
    /** @return texture id containing a complete rendered frame. Called only while the editor context is current. */
    public int renderScene(World world,int requestedWidth,int requestedHeight,Matrix4f projection,Matrix4f view){Objects.requireNonNull(world,"world");Objects.requireNonNull(projection,"projection");Objects.requireNonNull(view,"view");if(requestedWidth<1||requestedHeight<1)return displayTexture;if(program==0)initialize();resize(requestedWidth,requestedHeight);glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glViewport(0,0,width,height);glEnable(GL_DEPTH_TEST);glClearColor(.035f,.055f,.09f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glUseProgram(program);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,checkerTexture.id());glUniform1i(glGetUniformLocation(program,"uAlbedo"),0);glBindVertexArray(vao);for(var entry:world.view(MeshRenderer.class).entrySet()){Transform transform=world.get(entry.getKey(),Transform.class).orElse(null);if(transform==null)continue;try(MemoryStack stack=MemoryStack.stackPush()){FloatBuffer matrix=stack.mallocFloat(16);new Matrix4f(projection).mul(view).mul(model(transform)).get(matrix);glUniformMatrix4fv(glGetUniformLocation(program,"uMvp"),false,matrix);}Vec3 color=colorFor(entry.getValue().materialId());glUniform3f(glGetUniformLocation(program,"uTint"),(float)color.x(),(float)color.y(),(float)color.z());glDrawArrays(GL_TRIANGLES,0,36);}glBindVertexArray(0);glUseProgram(0);glBindFramebuffer(GL_READ_FRAMEBUFFER,framebuffer);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,displayFramebuffer);glBlitFramebuffer(0,0,width,height,0,0,width,height,GL_COLOR_BUFFER_BIT,GL_NEAREST);glBindFramebuffer(GL_FRAMEBUFFER,0);return displayTexture;}
    private Matrix4f model(Transform t){return new Matrix4f().translate((float)t.position().x(),(float)t.position().y(),(float)t.position().z()).rotateXYZ((float)Math.toRadians(t.rotationDegrees().x()),(float)Math.toRadians(t.rotationDegrees().y()),(float)Math.toRadians(t.rotationDegrees().z())).scale((float)t.scale().x(),(float)t.scale().y(),(float)t.scale().z());}
    private Vec3 colorFor(String material){return switch(material){case "pbr/metallic-blue"->new Vec3(.08,.35,.95);case "pbr/emissive-orange"->new Vec3(1,.25,.04);case "pbr/checker"->new Vec3(.2,.27,.34);default->new Vec3(.75,.75,.75);};}
    private void initialize(){vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,CUBE_VERTICES,GL_STATIC_DRAW);glVertexAttribPointer(0,3,GL_FLOAT,false,5*Float.BYTES,0);glEnableVertexAttribArray(0);glVertexAttribPointer(1,2,GL_FLOAT,false,5*Float.BYTES,3L*Float.BYTES);glEnableVertexAttribArray(1);glBindVertexArray(0);program=link(VERTEX,FRAGMENT);framebuffer=glGenFramebuffers();displayFramebuffer=glGenFramebuffers();sceneTexture=glGenTextures();displayTexture=glGenTextures();depthBuffer=glGenRenderbuffers();checkerTexture=Texture2D.checkerboard();}
    private void resize(int w,int h){if(width==w&&height==h)return;width=w;height=h;for(int texture:new int[]{sceneTexture,displayTexture}){glBindTexture(GL_TEXTURE_2D,texture);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,0L);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);}glBindRenderbuffer(GL_RENDERBUFFER,depthBuffer);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,width,height);glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,sceneTexture,0);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depthBuffer);if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Scene framebuffer is incomplete");glBindFramebuffer(GL_FRAMEBUFFER,displayFramebuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,displayTexture,0);if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Display framebuffer is incomplete");glBindFramebuffer(GL_FRAMEBUFFER,0);}
    private int link(String vs,String fs){int v=compile(GL_VERTEX_SHADER,vs),f=compile(GL_FRAGMENT_SHADER,fs),p=glCreateProgram();glAttachShader(p,v);glAttachShader(p,f);glLinkProgram(p);if(glGetProgrami(p,GL_LINK_STATUS)==GL_FALSE)throw new IllegalStateException(glGetProgramInfoLog(p));glDeleteShader(v);glDeleteShader(f);return p;}private int compile(int type,String source){int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException(glGetShaderInfoLog(shader));return shader;}
    @Override public void close(){if(checkerTexture!=null)checkerTexture.close();if(depthBuffer!=0)glDeleteRenderbuffers(depthBuffer);if(sceneTexture!=0)glDeleteTextures(sceneTexture);if(displayTexture!=0)glDeleteTextures(displayTexture);if(framebuffer!=0)glDeleteFramebuffers(framebuffer);if(displayFramebuffer!=0)glDeleteFramebuffers(displayFramebuffer);if(vbo!=0)glDeleteBuffers(vbo);if(vao!=0)glDeleteVertexArrays(vao);if(program!=0)glDeleteProgram(program);}
}
