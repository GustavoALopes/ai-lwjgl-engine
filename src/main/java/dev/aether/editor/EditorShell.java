package dev.aether.editor;

import dev.aether.core.AetherEngine;
import dev.aether.demo.DemoScenes;
import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.render.MeshRenderer;
import dev.aether.render.OpenGlRenderer;
import dev.aether.scene.Transform;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import dev.aether.platform.LwjglWindow;
import java.util.List;

/** ImGui editor host. All controls operate through the public ECS surface, which also makes them agent-addressable. */
public final class EditorShell implements AutoCloseable {
    private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();
    private final OpenGlRenderer renderer;
    private Entity selected;
    private boolean showDemo = true;
    public EditorShell(LwjglWindow window, AetherEngine engine, OpenGlRenderer renderer) {
        this.renderer = renderer;
        ImGui.createContext();
        ImGui.getIO().setIniFilename(null);
        glfw.init(window.handle(), true);
        gl3.init("#version 410");
        List<Entity> demo = DemoScenes.populateShowcase(engine.world());
        selected = demo.getFirst();
    }
    public void render(LwjglWindow window, AetherEngine engine) {
        glfw.newFrame(); ImGui.newFrame();
        drawMenu(window, engine); drawHierarchy(engine.world()); drawInspector(engine.world()); drawViewport(engine.world()); drawAssets(); drawConsole(engine.world());
        ImGui.render(); gl3.renderDrawData(ImGui.getDrawData());
    }
    private void drawMenu(LwjglWindow window, AetherEngine engine) {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) { ImGui.menuItem("New Scene", "Ctrl+N"); ImGui.menuItem("Open Scene...", "Ctrl+O"); ImGui.menuItem("Save Scene", "Ctrl+S"); ImGui.separator(); if (ImGui.menuItem("Quit")) window.requestClose(); ImGui.endMenu(); }
            if (ImGui.beginMenu("Game")) { boolean running = engine.state() == AetherEngine.State.RUNNING; ImGui.menuItem(running ? "Play (running)" : "Play", "F5", running, false); ImGui.menuItem("Pause", "F6", false, false); ImGui.endMenu(); }
            if (ImGui.beginMenu("View")) { showDemo = ImGui.menuItem("Demo Guide", null, showDemo); ImGui.menuItem("Scene Hierarchy", null, true, false); ImGui.menuItem("Inspector", null, true, false); ImGui.endMenu(); }
            if (ImGui.beginMenu("Help")) { ImGui.menuItem("Aether Engine 0.1.0"); ImGui.menuItem("Keyboard Shortcuts"); ImGui.endMenu(); }
            ImGui.endMainMenuBar();
        }
    }
    private void drawHierarchy(World world) { ImGui.setNextWindowPos(8, 28, ImGuiCond.Once); ImGui.setNextWindowSize(290, 480, ImGuiCond.Once); ImGui.begin("Scene Hierarchy"); for (Entity entity : world.entities()) { boolean active = entity.equals(selected); if (ImGui.selectable(world.nameOf(entity), active, ImGuiTreeNodeFlags.SpanAvailWidth)) selected = entity; } ImGui.end(); }
    private void drawInspector(World world) { ImGui.setNextWindowPos(8, 516, ImGuiCond.Once); ImGui.setNextWindowSize(290, 310, ImGuiCond.Once); ImGui.begin("Inspector"); if (selected == null) ImGui.textDisabled("Select an entity"); else { ImGui.text(world.nameOf(selected)); world.get(selected, Transform.class).ifPresent(transform -> { float[] position={(float)transform.position().x(),(float)transform.position().y(),(float)transform.position().z()}; if(ImGui.dragFloat3("Position",position,.05f)) world.add(selected,transform.withPosition(new dev.aether.math.Vec3(position[0],position[1],position[2]))); ImGui.text("Transform"); }); world.get(selected, MeshRenderer.class).ifPresent(mesh -> { ImGui.separator(); ImGui.text("Mesh: " + mesh.meshId()); ImGui.text("Material: " + mesh.materialId()); }); } ImGui.end(); }
    private void drawViewport(World world) { ImGui.setNextWindowPos(306, 28, ImGuiCond.Once); ImGui.setNextWindowSize(920, 798, ImGuiCond.Once); ImGui.begin("Scene View", ImGuiWindowFlags.NoScrollbar); float viewportWidth=ImGui.getContentRegionAvailX(), viewportHeight=ImGui.getContentRegionAvailY()-48; int texture=renderer.renderScene(world,Math.max(1,(int)viewportWidth),Math.max(1,(int)viewportHeight)); ImGui.image(texture,viewportWidth,viewportHeight,0,1,1,0); ImGui.textColored(.35f,.75f,1f,1f,"Demo Scene — live OpenGL framebuffer"); if(showDemo){ImGui.sameLine();ImGui.textDisabled("Select an entity to edit its Transform.");} ImGui.end(); }
    private void drawAssets() { ImGui.setNextWindowPos(1234, 28, ImGuiCond.Once); ImGui.setNextWindowSize(300, 390, ImGuiCond.Once); ImGui.begin("Asset Browser"); ImGui.bulletText("primitive/cube"); ImGui.bulletText("primitive/sphere"); ImGui.bulletText("pbr/metallic-blue"); ImGui.bulletText("pbr/emissive-orange"); ImGui.bulletText("assets/audio/ambient.ogg"); ImGui.end(); }
    private void drawConsole(World world) { ImGui.setNextWindowPos(1234, 426, ImGuiCond.Once); ImGui.setNextWindowSize(300, 400, ImGuiCond.Once); ImGui.begin("Console"); ImGui.textColored(.3f, 1f, .55f, 1f, "[Aether] Editor ready"); ImGui.text("Demo scene loaded: " + world.entityCount() + " entities"); ImGui.text("Physics + script systems enabled"); ImGui.end(); }
    @Override public void close() { gl3.dispose(); glfw.dispose(); ImGui.destroyContext(); }
}
