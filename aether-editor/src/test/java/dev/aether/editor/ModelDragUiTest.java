package dev.aether.editor;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelDragUiTest {
    @Test void realImGuiGestureDeliversOnceOnReleaseAndAllowsASecondDrop() {
        ImGui.createContext();
        try (var projects = new EditorProjects()) {
            ImGui.getIO().setIniFilename(null);
            ImGui.getIO().setDisplaySize(600, 400);
            ImGui.getIO().setDeltaTime(1f / 60);
            ImGui.getIO().getFonts().build();
            var drag = new ModelAssetDrag();
            frame(projects, drag, -100, -100, false);
            for (int gesture = 0; gesture < 2; gesture++) {
                assertNull(frame(projects, drag, 380, 20, false));
                assertNull(frame(projects, drag, 380, 20, true));
                assertNull(frame(projects, drag, 370, 35, true));
                assertNull(frame(projects, drag, 100, 100, true));
                assertNull(frame(projects, drag, 100, 100, true), "Hover must not import");
                assertEquals(Path.of("table.obj"), frame(projects, drag, 100, 100, false));
                for (int idle = 0; idle < 5; idle++)
                    assertNull(frame(projects, drag, 100, 100, false), "Release must not repeat the import");
            }
        } finally { ImGui.destroyContext(); }
    }

    private Path frame(EditorProjects projects, ModelAssetDrag drag, float x, float y, boolean down) {
        ImGui.getIO().setMousePos(x, y);
        ImGui.getIO().setMouseDown(0, down);
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(300, 300);
        ImGui.begin("Scene View", ImGuiWindowFlags.NoDecoration);
        ImGui.dummy(280, 280);
        Path delivered = drag.accept(projects.activeSession().orElseThrow());
        ImGui.end();
        ImGui.setNextWindowPos(350, 0);
        ImGui.setNextWindowSize(250, 300);
        ImGui.begin("Asset Browser", ImGuiWindowFlags.NoDecoration);
        ImGui.selectable("table.obj");
        drag.source(projects.activeSession().orElseThrow(), Path.of("table.obj"));
        ImGui.end();
        if (!ImGui.isMouseDown(0)) drag.cancel();
        ImGui.render();
        return delivered;
    }
}
