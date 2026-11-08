package dev.aether.render;

import dev.aether.game.GamePresentation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Desktop-only HUD adapter. Create, draw and close on the active render-context thread. */
final class NanoVgHudRenderer implements AutoCloseable {
    private long context;
    private ByteBuffer fontData;

    void render(GamePresentation presentation, float width, float height, float pixelRatio) {
        Objects.requireNonNull(presentation, "presentation");
        if (presentation.stats().isEmpty()) return;
        if (context == 0) init();
        nvgBeginFrame(context, width, height, pixelRatio);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NVGColor.malloc(stack);
            nvgFontFace(context, "hud");
            nvgTextAlign(context, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            for (var stat : presentation.stats()) {
                nvgFontSize(context, 36);
                float valueWidth = nvgTextBounds(context, 0, 0, stat.value(), (float[]) null);
                nvgFontSize(context, 12);
                float labelWidth = nvgTextBounds(context, 0, 0, stat.label(), (float[]) null);
                var panel = HudLayout.panel(stat.anchor(), width, height, Math.max(valueWidth, labelWidth));
                float scale = panel.scale(), center = panel.x() + panel.width() / 2;
                nvgBeginPath(context);
                nvgRoundedRect(context, panel.x(), panel.y(), panel.width(), panel.height(), 12 * scale);
                nvgFillColor(context, nvgRGBA((byte) 54, (byte) 69, (byte) 86, (byte) 255, color));
                nvgFill(context);
                nvgFontSize(context, 12 * scale);
                nvgFillColor(context, nvgRGBA((byte) 190, (byte) 203, (byte) 220, (byte) 255, color));
                nvgText(context, center, panel.y() + 22 * scale, stat.label());
                nvgFontSize(context, 36 * scale);
                nvgFillColor(context, nvgRGBA((byte) 255, (byte) 255, (byte) 255, (byte) 255, color));
                nvgText(context, center, panel.y() + 65 * scale, stat.value());
            }
            nvgEndFrame(context);
        } catch (RuntimeException | Error failure) {
            nvgCancelFrame(context);
            throw failure;
        }
    }

    private void init() {
        context = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (context == NULL) throw new IllegalStateException("Unable to create NanoVG HUD context");
        try (var stream = NanoVgHudRenderer.class.getResourceAsStream("/fonts/NotoSans.ttf")) {
            if (stream == null) throw new IllegalStateException("Missing bundled HUD font");
            byte[] bytes = stream.readAllBytes();
            fontData = memAlloc(bytes.length).put(bytes).flip();
            if (nvgCreateFontMem(context, "hud", fontData, false) == -1)
                throw new IllegalStateException("Unable to load HUD font");
        } catch (IOException | RuntimeException | Error failure) {
            close();
            throw new IllegalStateException("Unable to initialize HUD", failure);
        }
    }

    @Override public void close() {
        if (context != NULL) nvgDelete(context);
        context = NULL;
        if (fontData != null) memFree(fontData);
        fontData = null;
    }
}
