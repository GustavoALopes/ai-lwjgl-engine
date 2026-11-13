package dev.aether.editor;

import dev.aether.project.ProjectSession;
import imgui.ImGui;
import java.nio.file.Path;
import java.util.Objects;

/** Owns imgui-java's weakly referenced payload and consumes each gesture once. */
final class ModelAssetDrag {
    private static final String TYPE = "AETHER_OBJ";
    record Payload(ProjectSession session, Path path) { }
    private Payload pending;

    void source(ProjectSession session, Path path) {
        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload(TYPE, offer(session, path));
            ImGui.text("Drop " + path.getFileName() + " into Scene View");
            ImGui.endDragDropSource();
        }
    }

    Path accept(ProjectSession session) {
        if (!ImGui.beginDragDropTarget()) return null;
        try { return consume(ImGui.acceptDragDropPayload(TYPE), session); }
        finally { ImGui.endDragDropTarget(); }
    }

    Payload offer(ProjectSession session, Path path) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(path, "path");
        if (pending == null || pending.session() != session || !pending.path().equals(path))
            pending = new Payload(session, path);
        return pending;
    }

    Path consume(Payload payload, ProjectSession currentSession) {
        if (payload == null || payload != pending || payload.session() != currentSession) return null;
        Path path = pending.path();
        pending = null;
        return path;
    }

    boolean active() { return pending != null; }
    void cancel() { pending = null; }
}
