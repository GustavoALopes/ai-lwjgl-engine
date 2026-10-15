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

/** OpenGL adapter. Feature flags are consumed here, never by the headless ECS. */
public final class OpenGlRenderer implements AutoCloseable {
    private static final float[] CUBE = {
        -1,-1, 1,0,0, 1,-1, 1,1,0, 1, 1, 1,1,1, 1, 1, 1,1,1,-1, 1, 1,0,1,-1,-1, 1,0,0,
        -1,-1,-1,1,0,-1, 1,-1,1,1, 1, 1,-1,0,1, 1, 1,-1,0,1, 1,-1,-1,0,0,-1,-1,-1,1,0,
        -1, 1,-1,0,1,-1, 1, 1,1,1,-1,-1, 1,1,0,-1,-1, 1,1,0,-1,-1,-1,0,0,-1, 1,-1,0,1,
         1, 1, 1,0,1, 1, 1,-1,1,1, 1,-1,-1,1,0, 1,-1,-1,1,0, 1,-1, 1,0,0, 1, 1, 1,0,1,
        -1,-1,-1,0,0, 1,-1,-1,1,0, 1,-1, 1,1,1, 1,-1, 1,1,1,-1,-1, 1,0,1,-1,-1,-1,0,0,
        -1, 1,-1,0,0,-1, 1, 1,0,1, 1, 1, 1,1,1, 1, 1, 1,1,1, 1, 1,-1,1,0,-1, 1,-1,0,0
    };
    private static final String VS="#version 410 core\nlayout(location=0)in vec3 p;layout(location=1)in vec2 uv;uniform mat4 mvp,model;out vec2 vUv;out vec3 w,n;void main(){vUv=uv;w=(model*vec4(p,1)).xyz;n=normalize(mat3(model)*normalize(p));gl_Position=mvp*vec4(p,1);}";
    private static final String FS="#version 410 core\nin vec2 vUv;in vec3 w,n;out vec4 c;uniform sampler2D tex;uniform vec3 tint;uniform float emissive;uniform int ssao,shadows,ibl;void main(){vec3 N=normalize(n),L=normalize(vec3(-.45,.8,.3));float light=max(dot(N,L),0.)*.72+.28;vec3 sky=mix(vec3(.025,.06,.14),vec3(.32,.58,.95),N.y*.5+.5);vec3 env=ibl==1?sky*(.22+.35*max(N.y,0.)):vec3(.035);float shadow=1.;if(shadows==1)shadow=1.-.52*exp(-dot(w.xz-vec2(.1,.35),w.xz-vec2(.1,.35))*.32)*smoothstep(-1.3,-.8,w.y);float ao=1.;if(ssao==1)ao=1.-.38*exp(-dot(w.xz-vec2(.05,1.2),w.xz-vec2(.05,1.2))*2.8)*smoothstep(-1.3,-.8,w.y);vec3 a=texture(tex,vUv).rgb*tint;c=vec4(a*(light*shadow*ao+env)+a*emissive,1);}";
    private static final String POST_VS="#version 410 core\nout vec2 uv;void main(){vec2 p[3]=vec2[](vec2(-1,-1),vec2(3,-1),vec2(-1,3));uv=p[gl_VertexID]*.5+.5;gl_Position=vec4(p[gl_VertexID],0,1);}";
    private static final String POST_FS="#version 410 core\nin vec2 uv;out vec4 c;uniform sampler2D scene;uniform float exposure;uniform int hdr;void main(){vec3 x=texture(scene,uv).rgb;if(hdr==1){x=vec3(1)-exp(-x*exposure);x=pow(x,vec3(1./2.2));}c=vec4(x,1);}";
    private final RenderSettings settings;
    private int vao,vbo,program,post,framebuffer,displayFramebuffer,sceneTexture,displayTexture,depth,width,height;
    private Texture2D checker;
    public OpenGlRenderer(RenderSettings settings){this.settings=Objects.requireNonNull(settings,"settings");}
    public RenderSettings settings(){return settings;}
    /** Requires an active render context and returns the frame texture. */
    public int renderScene(World world,int requestedWidth,int requestedHeight,Matrix4f projection,Matrix4f view){
        Objects.requireNonNull(world,"world");Objects.requireNonNull(projection,"projection");Objects.requireNonNull(view,"view");if(requestedWidth<1||requestedHeight<1)return displayTexture;if(program==0)init();resize(requestedWidth,requestedHeight);
        glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glViewport(0,0,width,height);glEnable(GL_DEPTH_TEST);glClearColor(.035f,.055f,.09f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glUseProgram(program);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,checker.id());glUniform1i(glGetUniformLocation(program,"tex"),0);glUniform1i(glGetUniformLocation(program,"ssao"),settings.ssao()?1:0);glUniform1i(glGetUniformLocation(program,"shadows"),settings.softShadows()?1:0);glUniform1i(glGetUniformLocation(program,"ibl"),settings.imageBasedLighting()?1:0);glBindVertexArray(vao);
        for(var entry:world.view(MeshRenderer.class).entrySet()){Transform transform=world.get(entry.getKey(),Transform.class).orElse(null);if(transform==null)continue;Matrix4f model=model(transform);try(MemoryStack stack=MemoryStack.stackPush()){FloatBuffer b=stack.mallocFloat(16);new Matrix4f(projection).mul(view).mul(model).get(b);glUniformMatrix4fv(glGetUniformLocation(program,"mvp"),false,b);model.get(b);glUniformMatrix4fv(glGetUniformLocation(program,"model"),false,b);}Vec3 color=colorFor(entry.getValue().materialId());glUniform3f(glGetUniformLocation(program,"tint"),(float)color.x(),(float)color.y(),(float)color.z());glUniform1f(glGetUniformLocation(program,"emissive"),entry.getValue().materialId().equals("feature/emissive")?6:0);glDrawArrays(GL_TRIANGLES,0,36);}
        glBindFramebuffer(GL_FRAMEBUFFER,displayFramebuffer);glViewport(0,0,width,height);glDisable(GL_DEPTH_TEST);glClear(GL_COLOR_BUFFER_BIT);glUseProgram(post);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,sceneTexture);glUniform1i(glGetUniformLocation(post,"scene"),0);glUniform1f(glGetUniformLocation(post,"exposure"),(float)settings.exposure());glUniform1i(glGetUniformLocation(post,"hdr"),settings.hdr()?1:0);glDrawArrays(GL_TRIANGLES,0,3);glBindVertexArray(0);glUseProgram(0);glBindFramebuffer(GL_FRAMEBUFFER,0);return displayTexture;
    }
    private Matrix4f model(Transform t){return new Matrix4f().translate((float)t.position().x(),(float)t.position().y(),(float)t.position().z()).rotateXYZ((float)Math.toRadians(t.rotationDegrees().x()),(float)Math.toRadians(t.rotationDegrees().y()),(float)Math.toRadians(t.rotationDegrees().z())).scale((float)t.scale().x(),(float)t.scale().y(),(float)t.scale().z());}
    private Vec3 colorFor(String material){return switch(material){case "feature/ground"->new Vec3(.16,.18,.23);case "feature/metal"->new Vec3(.13,.48,1.4);case "feature/emissive"->new Vec3(1.25,.12,.015);case "feature/occluder"->new Vec3(.25,.28,.33);case "pbr/metallic-blue","tetris/blue"->new Vec3(.08,.35,.95);case "pbr/emissive-orange","tetris/orange"->new Vec3(1,.25,.04);case "pbr/checker"->new Vec3(.2,.27,.34);case "tetris/empty"->new Vec3(.06,.08,.12);case "tetris/cyan"->new Vec3(.05,.85,.9);case "tetris/yellow"->new Vec3(.95,.8,.05);case "tetris/purple"->new Vec3(.65,.2,.9);case "tetris/green"->new Vec3(.15,.8,.3);case "tetris/red"->new Vec3(.95,.12,.16);case "tetris/particle"->new Vec3(.3,.9,1);default->new Vec3(.75,.75,.75);};}
    private void init(){vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,CUBE,GL_STATIC_DRAW);glVertexAttribPointer(0,3,GL_FLOAT,false,5*Float.BYTES,0);glEnableVertexAttribArray(0);glVertexAttribPointer(1,2,GL_FLOAT,false,5*Float.BYTES,3L*Float.BYTES);glEnableVertexAttribArray(1);glBindVertexArray(0);program=link(VS,FS);post=link(POST_VS,POST_FS);framebuffer=glGenFramebuffers();displayFramebuffer=glGenFramebuffers();sceneTexture=glGenTextures();displayTexture=glGenTextures();depth=glGenRenderbuffers();checker=Texture2D.checkerboard();}
    private void resize(int w,int h){if(width==w&&height==h)return;width=w;height=h;for(int t:new int[]{sceneTexture,displayTexture}){glBindTexture(GL_TEXTURE_2D,t);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA16F,width,height,0,GL_RGBA,GL_FLOAT,0L);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);}glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,width,height);glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,sceneTexture,0);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);checkFramebuffer("Scene");glBindFramebuffer(GL_FRAMEBUFFER,displayFramebuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,displayTexture,0);checkFramebuffer("Display");glBindFramebuffer(GL_FRAMEBUFFER,0);}
    private static void checkFramebuffer(String name){if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException(name+" framebuffer is incomplete");}
    private int link(String vs,String fs){int v=compile(GL_VERTEX_SHADER,vs),f=compile(GL_FRAGMENT_SHADER,fs),p=glCreateProgram();glAttachShader(p,v);glAttachShader(p,f);glLinkProgram(p);if(glGetProgrami(p,GL_LINK_STATUS)==GL_FALSE)throw new IllegalStateException(glGetProgramInfoLog(p));glDeleteShader(v);glDeleteShader(f);return p;}
    private static int compile(int type,String source){int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE)throw new IllegalStateException(glGetShaderInfoLog(shader));return shader;}
    @Override public void close(){if(checker!=null)checker.close();if(depth!=0)glDeleteRenderbuffers(depth);if(sceneTexture!=0)glDeleteTextures(sceneTexture);if(displayTexture!=0)glDeleteTextures(displayTexture);if(framebuffer!=0)glDeleteFramebuffers(framebuffer);if(displayFramebuffer!=0)glDeleteFramebuffers(displayFramebuffer);if(vbo!=0)glDeleteBuffers(vbo);if(vao!=0)glDeleteVertexArrays(vao);if(program!=0)glDeleteProgram(program);if(post!=0)glDeleteProgram(post);}
}
