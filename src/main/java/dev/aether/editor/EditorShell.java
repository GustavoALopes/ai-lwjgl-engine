package dev.aether.editor;

import dev.aether.core.AetherEngine;
import dev.aether.demo.DemoScenes;
import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.render.MeshRenderer;
import dev.aether.render.OpenGlRenderer;
import dev.aether.scene.Transform;
import imgui.ImGui;
import imgui.ImDrawList;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import dev.aether.platform.LwjglWindow;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.List;

/** ImGui editor host. All controls operate through the public ECS surface, which also makes them agent-addressable. */
public final class EditorShell implements AutoCloseable {
    private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();
    private final OpenGlRenderer renderer;
    private Entity selected;
    private Entity draggingEntity;
    private Vec3 dragStartPosition;
    private float dragStartMouseX;
    private float dragStartMouseY;
    private Axis dragAxis;
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
    private void drawHierarchy(World world) { ImGui.setNextWindowPos(8, 28, ImGuiCond.Once); ImGui.setNextWindowSize(290, 480, ImGuiCond.Once); ImGui.begin("Scene Hierarchy"); if(ImGui.button("+ Create Cube")){Entity entity=world.create("User Cube");world.add(entity,Transform.identity());world.add(entity,new MeshRenderer("primitive/cube","pbr/metallic-blue"));selected=entity;} ImGui.sameLine(); if(selected!=null&&ImGui.button("Delete")&&world.exists(selected)){world.destroy(selected);selected=null;} ImGui.separator(); for (Entity entity : world.entities()) { boolean active = entity.equals(selected); if (ImGui.selectable(world.nameOf(entity), active, ImGuiTreeNodeFlags.SpanAvailWidth)) selected = entity; } ImGui.end(); }
    private void drawInspector(World world) { ImGui.setNextWindowPos(8, 516, ImGuiCond.Once); ImGui.setNextWindowSize(290, 310, ImGuiCond.Once); ImGui.begin("Inspector"); if (selected == null) ImGui.textDisabled("Select an entity"); else { ImGui.text(world.nameOf(selected)); world.get(selected, Transform.class).ifPresent(transform -> { float[] position={(float)transform.position().x(),(float)transform.position().y(),(float)transform.position().z()}; if(ImGui.dragFloat3("Position",position,.05f)) world.add(selected,transform.withPosition(new dev.aether.math.Vec3(position[0],position[1],position[2]))); ImGui.text("Transform"); }); world.get(selected, MeshRenderer.class).ifPresent(mesh -> { ImGui.separator(); ImGui.text("Mesh: " + mesh.meshId()); ImGui.text("Material: " + mesh.materialId()); }); } ImGui.end(); }
    private void drawViewport(World world) { ImGui.setNextWindowPos(306, 28, ImGuiCond.Once); ImGui.setNextWindowSize(920, 798, ImGuiCond.Once); ImGui.begin("Scene View", ImGuiWindowFlags.NoScrollbar); float viewportWidth=ImGui.getContentRegionAvailX(), viewportHeight=ImGui.getContentRegionAvailY()-48; int texture=renderer.renderScene(world,Math.max(1,(int)viewportWidth),Math.max(1,(int)viewportHeight)); ImGui.image(texture,viewportWidth,viewportHeight,0,1,1,0); float left=ImGui.getItemRectMinX(), top=ImGui.getItemRectMinY(); if(ImGui.isItemHovered()&&ImGui.isMouseClicked(0)){Entity hit=pick(world,left,top,viewportWidth,viewportHeight,ImGui.getMousePosX(),ImGui.getMousePosY());if(hit!=null)selected=hit;} drawGizmo(world,left,top,viewportWidth,viewportHeight); ImGui.textColored(.35f,.75f,1f,1f,"Demo Scene — click a cube to select; drag red/green gizmo axis to move it."); ImGui.end(); }
    private Entity pick(World world,float left,float top,float width,float height,float mouseX,float mouseY){Entity nearest=null;float nearestDistance=Float.MAX_VALUE;for(Entity entity:world.view(MeshRenderer.class).keySet()){Transform transform=world.get(entity,Transform.class).orElse(null);if(transform==null)continue;float[] point=project(transform.position(),left,top,width,height);float distance=(point[0]-mouseX)*(point[0]-mouseX)+(point[1]-mouseY)*(point[1]-mouseY);if(distance<50*50&&distance<nearestDistance){nearest=entity;nearestDistance=distance;}}return nearest;}
    private void drawGizmo(World world,float left,float top,float width,float height){if(selected==null||!world.exists(selected))return;Transform transform=world.get(selected,Transform.class).orElse(null);if(transform==null)return;float[] point=project(transform.position(),left,top,width,height);float x=point[0],y=point[1];ImDrawList draw=ImGui.getWindowDrawList();draw.addRect(x-26,y-26,x+26,y+26,0xFFFFFFFF,0,0,2);draw.addLine(x,y,x+54,y,0xFF3030FF,3);draw.addLine(x,y,x,y-54,0xFF30FF30,3);draw.addText(x+57,y-8,0xFF3030FF,"X");draw.addText(x-7,y-70,0xFF30FF30,"Y");if(ImGui.isMouseClicked(0)){float mouseX=ImGui.getMousePosX(),mouseY=ImGui.getMousePosY();if(Math.abs(mouseY-y)<14&&mouseX>=x&&mouseX<=x+64)beginDrag(Axis.X,transform);else if(Math.abs(mouseX-x)<14&&mouseY<=y&&mouseY>=y-64)beginDrag(Axis.Y,transform);}if(draggingEntity!=null){if(ImGui.isMouseDown(0)){double delta=dragAxis==Axis.X?(ImGui.getMousePosX()-dragStartMouseX)/50d:(dragStartMouseY-ImGui.getMousePosY())/50d;Vec3 start=dragStartPosition;Vec3 position=dragAxis==Axis.X?new Vec3(start.x()+delta,start.y(),start.z()):new Vec3(start.x(),start.y()+delta,start.z());world.add(draggingEntity,transform.withPosition(position));}else{draggingEntity=null;dragAxis=null;}}}
    private void beginDrag(Axis axis,Transform transform){draggingEntity=selected;dragAxis=axis;dragStartPosition=transform.position();dragStartMouseX=ImGui.getMousePosX();dragStartMouseY=ImGui.getMousePosY();}
    private float[] project(Vec3 position,float left,float top,float width,float height){Matrix4f projection=new Matrix4f().perspective((float)Math.toRadians(55),width/height,.1f,100);Matrix4f view=new Matrix4f().lookAt(9,7,11,0,0,0,0,1,0);Vector4f clip=projection.mul(view,new Matrix4f()).transform(new Vector4f((float)position.x(),(float)position.y(),(float)position.z(),1));float ndcX=clip.x/clip.w,ndcY=clip.y/clip.w;return new float[]{left+(ndcX*.5f+.5f)*width,top+(1-(ndcY*.5f+.5f))*height};}
    private enum Axis { X, Y }
    private void drawAssets() { ImGui.setNextWindowPos(1234, 28, ImGuiCond.Once); ImGui.setNextWindowSize(300, 390, ImGuiCond.Once); ImGui.begin("Asset Browser"); ImGui.bulletText("primitive/cube"); ImGui.bulletText("primitive/sphere"); ImGui.bulletText("pbr/metallic-blue"); ImGui.bulletText("pbr/emissive-orange"); ImGui.bulletText("assets/audio/ambient.ogg"); ImGui.end(); }
    private void drawConsole(World world) { ImGui.setNextWindowPos(1234, 426, ImGuiCond.Once); ImGui.setNextWindowSize(300, 400, ImGuiCond.Once); ImGui.begin("Console"); ImGui.textColored(.3f, 1f, .55f, 1f, "[Aether] Editor ready"); ImGui.text("Demo scene loaded: " + world.entityCount() + " entities"); ImGui.text("Physics + script systems enabled"); ImGui.end(); }
    @Override public void close() { gl3.dispose(); glfw.dispose(); ImGui.destroyContext(); }
}
