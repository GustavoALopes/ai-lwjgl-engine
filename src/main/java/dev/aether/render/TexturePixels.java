package dev.aether.render;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Immutable RGBA8 pixel data that can be unit tested independently from an OpenGL context. */
public final class TexturePixels {
    private final int width, height;
    private final byte[] rgba;
    public TexturePixels(int width, int height, byte[] rgba) { if(width<=0||height<=0)throw new IllegalArgumentException("Texture dimensions must be positive"); Objects.requireNonNull(rgba); if(rgba.length!=width*height*4)throw new IllegalArgumentException("RGBA data length does not match dimensions"); this.width=width;this.height=height;this.rgba=rgba.clone(); }
    public int width(){return width;} public int height(){return height;} public byte[] rgba(){return rgba.clone();}
    public ByteBuffer buffer(){return ByteBuffer.wrap(rgba).asReadOnlyBuffer();}
    public static TexturePixels checkerboard(int size,int firstRgba,int secondRgba){if(size<2)throw new IllegalArgumentException("Checkerboard size must be at least 2");byte[] pixels=new byte[size*size*4];for(int y=0;y<size;y++)for(int x=0;x<size;x++){int color=((x/16+y/16)&1)==0?firstRgba:secondRgba;int index=(y*size+x)*4;pixels[index]=(byte)(color>>>24);pixels[index+1]=(byte)(color>>>16);pixels[index+2]=(byte)(color>>>8);pixels[index+3]=(byte)color;}return new TexturePixels(size,size,pixels);}
}
