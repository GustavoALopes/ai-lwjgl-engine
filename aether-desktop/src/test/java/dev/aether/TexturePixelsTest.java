package dev.aether;

import dev.aether.render.TexturePixels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TexturePixelsTest {
    @Test void checkerboardCreatesRgbaPixelsAndDefensivelyCopiesThem() {
        TexturePixels texture = TexturePixels.checkerboard(32, 0xFFFFFFFF, 0x000000FF);
        assertEquals(32, texture.width());
        assertEquals(32 * 32 * 4, texture.rgba().length);
        byte[] copy = texture.rgba();
        copy[0] = 0;
        assertNotEquals(0, texture.rgba()[0]);
    }

    @Test void rejectsMismatchedPixelData() {
        assertThrows(IllegalArgumentException.class, () -> new TexturePixels(2, 2, new byte[3]));
    }
}
