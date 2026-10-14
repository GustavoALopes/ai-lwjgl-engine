package dev.aether.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/** OpenGL RGBA texture. Construct and close only while its owning context is current. */
public final class Texture2D implements AutoCloseable {
    private final int id;
    private Texture2D(TexturePixels pixels){id=glGenTextures();glBindTexture(GL_TEXTURE_2D,id);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_REPEAT);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_REPEAT);ByteBuffer data=BufferUtils.createByteBuffer(pixels.rgba().length).put(pixels.rgba()).flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,pixels.width(),pixels.height(),0,GL_RGBA,GL_UNSIGNED_BYTE,data);glGenerateMipmap(GL_TEXTURE_2D);}
    public static Texture2D checkerboard(){return new Texture2D(TexturePixels.checkerboard(128,0xE8EEF8FF,0x536579FF));}
    public static Texture2D fromFile(Path file){try(MemoryStack stack=MemoryStack.stackPush()){IntBuffer width=stack.mallocInt(1),height=stack.mallocInt(1),channels=stack.mallocInt(1);STBImage.stbi_set_flip_vertically_on_load(true);ByteBuffer data=STBImage.stbi_load(file.toString(),width,height,channels,4);if(data==null)throw new IllegalArgumentException("Unable to load texture "+file+": "+STBImage.stbi_failure_reason());try{byte[] bytes=new byte[width.get(0)*height.get(0)*4];data.get(bytes);return new Texture2D(new TexturePixels(width.get(0),height.get(0),bytes));}finally{STBImage.stbi_image_free(data);}}}
    public int id(){return id;} @Override public void close(){glDeleteTextures(id);}
}
