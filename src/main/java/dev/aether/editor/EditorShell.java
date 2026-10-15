package dev.aether.editor;

import dev.aether.core.AetherEngine;
import dev.aether.demo.DemoScenes;
import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.render.MeshRenderer;
import dev.aether.render.OpenGlRenderer;
import dev.aether.script.Script;
import dev.aether.script.ScriptComponent;
import dev.aether.scene.Transform;
import dev.aether.scene.Tags;
import imgui.ImGui;
import imgui.ImDrawList;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImString;
import dev.aether.platform.LwjglWindow;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import static org.lwjgl.glfw.GLFW.*;

/** ImGui editor host. All controls operate through the public ECS surface, which also makes them agent-addressable. */
public final class EditorShell implements AutoCloseable {
    private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();
    private final OpenGlRenderer renderer;
    private Entity selected;
    private Entity inspectorEntity;
    private final ImString inspectorName = new ImString(128);
    private final ImString inspectorTags = new ImString(256);
    private Entity draggingEntity;
    private Vec3 dragStartPosition;
    private Vec3 dragStartScale;
    private float dragStartMouseX;
    private float dragStartMouseY;
    private Axis dragAxis;
    private TransformMode transformMode = TransformMode.TRANSLATE;
    private boolean showDemo = true;
    private final SceneCamera sceneCamera = SceneCamera.defaultCamera();
    private SceneViewMode sceneViewMode = SceneViewMode.THREE_D;
    private double previousSceneMouseX;
    private double previousSceneMouseY;
    private boolean hasPreviousSceneMouse;
    private boolean rightNavigating;
    private boolean middleNavigating;
    private boolean leftNavigating;
    public EditorShell(LwjglWindow window, AetherEngine engine, OpenGlRenderer renderer) {
        this.renderer = renderer;
        ImGui.createContext();
        ImGui.getIO().setIniFilename(null);
        glfw.init(window.handle(), true);
        gl3.init("#version 410");
        DemoScenes.populateShowcase(engine.world());
    }
    public void render(LwjglWindow window, AetherEngine engine) {
        glfw.newFrame(); ImGui.newFrame();
        drawMenu(window, engine); drawHierarchy(engine.world()); drawInspector(engine.world()); drawViewport(window,engine.world()); drawAssets(); drawConsole(engine.world());
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
    private void drawHierarchy(World world) { ImGui.setNextWindowPos(8, 28, ImGuiCond.Always); ImGui.setNextWindowSize(290, 480, ImGuiCond.Always); ImGui.begin("Scene Hierarchy",editorPanelFlags()); if(ImGui.button("+ Create Cube")){Entity entity=world.create("User Cube");world.add(entity,Transform.identity());world.add(entity,new MeshRenderer("primitive/cube","pbr/metallic-blue"));selected=entity;} ImGui.sameLine(); if(selected!=null&&ImGui.button("Delete")&&world.exists(selected)){world.destroy(selected);selected=null;} ImGui.separator(); for (Entity entity : world.entities()) { boolean active = entity.equals(selected); if (ImGui.selectable(world.nameOf(entity), active, ImGuiTreeNodeFlags.SpanAvailWidth)) selected = entity; } ImGui.end(); }
    private void drawInspector(World world) { ImGui.setNextWindowPos(8, 516, ImGuiCond.Always); ImGui.setNextWindowSize(290, 310, ImGuiCond.Always); ImGui.begin("Inspector",editorPanelFlags()); if (selected == null || !world.exists(selected)) { ImGui.textDisabled("Select an object in Scene View or Scene Hierarchy."); } else { syncInspectorFields(world); ImGui.textDisabled("Type: " + objectType(world)); if(ImGui.inputText("Name",inspectorName) && !inspectorName.get().isBlank()) world.rename(selected,inspectorName.get()); if(ImGui.inputText("Tags (comma separated)",inspectorTags)) world.add(selected,new Tags(parseTags(inspectorTags.get()))); ImGui.separator(); if(ImGui.button("Move"))transformMode=TransformMode.TRANSLATE; ImGui.sameLine(); if(ImGui.button("Scale"))transformMode=TransformMode.SCALE; ImGui.sameLine(); ImGui.textDisabled("Active: "+transformMode.label); world.get(selected, Transform.class).ifPresent(transform -> { float[] position={(float)transform.position().x(),(float)transform.position().y(),(float)transform.position().z()}; if(ImGui.dragFloat3("Position",position,.05f)) world.add(selected,transform.withPosition(new dev.aether.math.Vec3(position[0],position[1],position[2]))); float[] scale={(float)transform.scale().x(),(float)transform.scale().y(),(float)transform.scale().z()}; if(ImGui.dragFloat3("Scale",scale,.02f,.05f,100f))world.add(selected,transform.withScale(new Vec3(scale[0],scale[1],scale[2]))); ImGui.text("Transform Gizmo: "+transformMode.label); }); world.get(selected, MeshRenderer.class).ifPresent(mesh -> { ImGui.separator(); ImGui.text("Mesh: " + mesh.meshId()); ImGui.text("Material: " + mesh.materialId()); }); ImGui.separator(); ScriptComponent script=world.get(selected,ScriptComponent.class).orElse(null); if(script==null){if(ImGui.button("Add Script Component"))world.add(selected,new ScriptComponent(new EditorScript()));}else ImGui.text("Script: " + script.script().getClass().getSimpleName()); ImGui.textDisabled("Components: " + world.componentsOf(selected).stream().map(component->component.getClass().getSimpleName()).reduce((a,b)->a+", "+b).orElse("None")); } ImGui.end(); }
    private void syncInspectorFields(World world){if(selected.equals(inspectorEntity))return;inspectorEntity=selected;inspectorName.set(world.nameOf(selected));inspectorTags.set(String.join(", ",world.get(selected,Tags.class).orElse(Tags.none()).values()));}
    private String objectType(World world){if(world.get(selected,MeshRenderer.class).isPresent())return "Mesh Object";if(world.get(selected,ScriptComponent.class).isPresent())return "Scripted Entity";return "Entity";}
    private static Set<String> parseTags(String source){LinkedHashSet<String> tags=new LinkedHashSet<>();Arrays.stream(source.split(",")).map(String::trim).filter(value->!value.isEmpty()).forEach(tags::add);return tags;}
    private static final class EditorScript implements Script { }
    private void drawViewport(LwjglWindow window,World world) { ImGui.setNextWindowPos(306, 28, ImGuiCond.Always); ImGui.setNextWindowSize(920, 798, ImGuiCond.Always); ImGui.begin("Scene View", ImGuiWindowFlags.NoScrollbar|editorPanelFlags()); if(ImGui.button("3D",sceneViewMode==SceneViewMode.THREE_D?58:48,0))sceneViewMode=SceneViewMode.THREE_D; ImGui.sameLine(); if(ImGui.button("2D",sceneViewMode==SceneViewMode.TWO_D?58:48,0))sceneViewMode=SceneViewMode.TWO_D; ImGui.sameLine(); ImGui.textDisabled(sceneViewMode==SceneViewMode.THREE_D?"Perspective":"Orthographic"); ImGui.separator(); float viewportWidth=ImGui.getContentRegionAvailX(), viewportHeight=ImGui.getContentRegionAvailY()-28; Matrix4f projection=projection(viewportWidth,viewportHeight); int texture=renderer.renderScene(world,Math.max(1,(int)viewportWidth),Math.max(1,(int)viewportHeight),projection,viewMatrix()); ImGui.image(texture,viewportWidth,viewportHeight,0,1,1,0); float left=ImGui.getItemRectMinX(), top=ImGui.getItemRectMinY(); boolean hovered=ImGui.isItemHovered(); navigateViewport(window,hovered); if(hovered&&ImGui.isMouseClicked(0)&&!window.isKeyDown(GLFW_KEY_LEFT_ALT)&&!window.isKeyDown(GLFW_KEY_RIGHT_ALT)){Entity hit=pick(world,left,top,viewportWidth,viewportHeight,ImGui.getMousePosX(),ImGui.getMousePosY());if(hit!=null)selected=hit;} drawGizmo(world,left,top,viewportWidth,viewportHeight); ImGui.textColored(.35f,.75f,1f,1f,sceneViewMode==SceneViewMode.THREE_D?"Navigate: RMB + WASD/QE fly | Alt + LMB orbit | MMB pan | wheel zoom":"2D orthographic view: MMB pan | wheel zoom"); ImGui.end(); }
    private void navigateViewport(LwjglWindow window,boolean hovered){LwjglWindow.CursorPosition mouse=window.cursorPosition();double dx=hasPreviousSceneMouse?mouse.x()-previousSceneMouseX:0,dy=hasPreviousSceneMouse?mouse.y()-previousSceneMouseY:0;previousSceneMouseX=mouse.x();previousSceneMouseY=mouse.y();hasPreviousSceneMouse=true;boolean right=window.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT),middle=window.isMouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE),left=window.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT),alt=window.isKeyDown(GLFW_KEY_LEFT_ALT)||window.isKeyDown(GLFW_KEY_RIGHT_ALT);if(!right)rightNavigating=false;if(!middle)middleNavigating=false;if(!left)leftNavigating=false;if(hovered&&right)rightNavigating=true;if(hovered&&middle)middleNavigating=true;if(hovered&&left&&alt)leftNavigating=true;if(sceneViewMode==SceneViewMode.TWO_D){if(middleNavigating)sceneCamera.panOrthographic(-dx*.02,dy*.02);}else{if(rightNavigating){sceneCamera.orbit(-dx*.01,dy*.01);double speed=window.isKeyDown(GLFW_KEY_LEFT_SHIFT)||window.isKeyDown(GLFW_KEY_RIGHT_SHIFT)?.25:.08;if(window.isKeyDown(GLFW_KEY_W))sceneCamera.fly(0,0,speed);if(window.isKeyDown(GLFW_KEY_S))sceneCamera.fly(0,0,-speed);if(window.isKeyDown(GLFW_KEY_A))sceneCamera.fly(-speed,0,0);if(window.isKeyDown(GLFW_KEY_D))sceneCamera.fly(speed,0,0);if(window.isKeyDown(GLFW_KEY_Q))sceneCamera.fly(0,-speed,0);if(window.isKeyDown(GLFW_KEY_E))sceneCamera.fly(0,speed,0);}if(leftNavigating)sceneCamera.orbit(-dx*.01,dy*.01);if(middleNavigating)sceneCamera.pan(-dx*.02,dy*.02);}float wheel=ImGui.getIO().getMouseWheel();if(hovered&&wheel!=0)sceneCamera.dolly(wheel*Math.max(.1,distanceToFocus()*.12));}
    private double distanceToFocus(){Vec3 p=sceneCamera.position(),t=sceneCamera.target();double x=p.x()-t.x(),y=p.y()-t.y(),z=p.z()-t.z();return Math.sqrt(x*x+y*y+z*z);}
    private Entity pick(World world,float left,float top,float width,float height,float mouseX,float mouseY){Entity nearest=null;float nearestDistance=Float.MAX_VALUE;for(Entity entity:world.view(MeshRenderer.class).keySet()){Transform transform=world.get(entity,Transform.class).orElse(null);if(transform==null)continue;float[] point=project(transform.position(),left,top,width,height);float distance=(point[0]-mouseX)*(point[0]-mouseX)+(point[1]-mouseY)*(point[1]-mouseY);if(distance<50*50&&distance<nearestDistance){nearest=entity;nearestDistance=distance;}}return nearest;}
    private void drawGizmo(World world,float left,float top,float width,float height){if(selected==null||!world.exists(selected))return;Transform transform=world.get(selected,Transform.class).orElse(null);if(transform==null)return;float[] point=project(transform.position(),left,top,width,height);float x=point[0],y=point[1];ImDrawList draw=ImGui.getWindowDrawList();draw.addRect(x-26,y-26,x+26,y+26,0xFFFFFFFF,0,0,2);draw.addLine(x,y,x+54,y,0xFF3030FF,3);draw.addLine(x,y,x,y-54,0xFF30FF30,3);draw.addText(x+57,y-8,0xFF3030FF,transformMode==TransformMode.TRANSLATE?"X":"Scale X");draw.addText(x-7,y-70,0xFF30FF30,transformMode==TransformMode.TRANSLATE?"Y":"Scale Y");if(ImGui.isMouseClicked(0)){float mouseX=ImGui.getMousePosX(),mouseY=ImGui.getMousePosY();if(Math.abs(mouseY-y)<14&&mouseX>=x&&mouseX<=x+64)beginDrag(Axis.X,transform);else if(Math.abs(mouseX-x)<14&&mouseY<=y&&mouseY>=y-64)beginDrag(Axis.Y,transform);}if(draggingEntity!=null){if(ImGui.isMouseDown(0)){double delta=dragAxis==Axis.X?(ImGui.getMousePosX()-dragStartMouseX)/50d:(dragStartMouseY-ImGui.getMousePosY())/50d;Vec3 start=dragStartPosition;if(transformMode==TransformMode.TRANSLATE){Vec3 position=dragAxis==Axis.X?new Vec3(start.x()+delta,start.y(),start.z()):new Vec3(start.x(),start.y()+delta,start.z());world.add(draggingEntity,transform.withPosition(position));}else{Vec3 scale=dragAxis==Axis.X?new Vec3(Math.max(.05,dragStartScale.x()+delta),dragStartScale.y(),dragStartScale.z()):new Vec3(dragStartScale.x(),Math.max(.05,dragStartScale.y()+delta),dragStartScale.z());world.add(draggingEntity,transform.withScale(scale));}}else{draggingEntity=null;dragAxis=null;}}}
    private void beginDrag(Axis axis,Transform transform){draggingEntity=selected;dragAxis=axis;dragStartPosition=transform.position();dragStartScale=transform.scale();dragStartMouseX=ImGui.getMousePosX();dragStartMouseY=ImGui.getMousePosY();}
    private Matrix4f projection(float width,float height){float aspect=Math.max(1,width)/Math.max(1,height);if(sceneViewMode==SceneViewMode.THREE_D)return new Matrix4f().perspective((float)Math.toRadians(55),aspect,.1f,100);float halfHeight=(float)(sceneCamera.focusDistance()*Math.tan(Math.toRadians(55)/2));return new Matrix4f().ortho(-halfHeight*aspect,halfHeight*aspect,-halfHeight,halfHeight,.1f,100);}
    private Matrix4f viewMatrix(){return sceneViewMode==SceneViewMode.THREE_D?sceneCamera.viewMatrix():sceneCamera.orthographicViewMatrix();}
    private float[] project(Vec3 position,float left,float top,float width,float height){Vector4f clip=projection(width,height).mul(viewMatrix()).transform(new Vector4f((float)position.x(),(float)position.y(),(float)position.z(),1));float ndcX=clip.x/clip.w,ndcY=clip.y/clip.w;return new float[]{left+(ndcX*.5f+.5f)*width,top+(1-(ndcY*.5f+.5f))*height};}
    private enum Axis { X, Y }
    private enum SceneViewMode { THREE_D, TWO_D }
    private enum TransformMode { TRANSLATE("Move"), SCALE("Scale"); private final String label; TransformMode(String label){this.label=label;} }
    private void drawAssets() { ImGui.setNextWindowPos(1234, 28, ImGuiCond.Always); ImGui.setNextWindowSize(300, 390, ImGuiCond.Always); ImGui.begin("Asset Browser",editorPanelFlags()); ImGui.bulletText("primitive/cube"); ImGui.bulletText("primitive/sphere"); ImGui.bulletText("pbr/metallic-blue"); ImGui.bulletText("pbr/emissive-orange"); ImGui.bulletText("assets/audio/ambient.ogg"); ImGui.end(); }
    private void drawConsole(World world) { ImGui.setNextWindowPos(1234, 426, ImGuiCond.Always); ImGui.setNextWindowSize(300, 400, ImGuiCond.Always); ImGui.begin("Console",editorPanelFlags()); ImGui.textColored(.3f, 1f, .55f, 1f, "[Aether] Editor ready"); ImGui.text("Demo scene loaded: " + world.entityCount() + " entities"); ImGui.text("Physics + script systems enabled"); ImGui.end(); }
    private int editorPanelFlags(){return ImGuiWindowFlags.NoMove|ImGuiWindowFlags.NoResize|ImGuiWindowFlags.NoCollapse;}
    @Override public void close() { gl3.dispose(); glfw.dispose(); ImGui.destroyContext(); }
}
