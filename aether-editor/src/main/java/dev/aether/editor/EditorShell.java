package dev.aether.editor;

import dev.aether.core.AetherEngine;
import dev.aether.game.GameKey;
import dev.aether.game.GamePresentation;
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
import dev.aether.platform.DesktopApplication;
import dev.aether.platform.LwjglGameKeys;
import dev.aether.project.ProjectSession;
import dev.aether.project.AssetCatalog;
import java.io.IOException;
import java.nio.file.Path;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;
import java.util.EnumSet;
import java.util.List;
import static org.lwjgl.glfw.GLFW.*;

/**
 * ImGui editor host. All controls operate through the public ECS surface, which
 * also makes them agent-addressable.
 */
public final class EditorShell implements DesktopApplication {
    private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();
    private final OpenGlRenderer renderer;
    private final EditorProjects projects;
    private final World emptyWorld = new World();
    private ProjectSession displayedSession;
    private final ImString projectPath = new ImString(2048);
    private boolean showOpenProject;
    private boolean showNewProject;
    private final ImString newProjectName = new ImString(128);
    private final ImString newProjectFile = new ImString(2048);
    private String projectDialogError;
    private boolean closed;
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
    private SceneCamera sceneCamera = SceneCamera.defaultCamera();
    private SceneViewMode sceneViewMode = SceneViewMode.THREE_D;
    private double previousSceneMouseX;
    private double previousSceneMouseY;
    private boolean hasPreviousSceneMouse;
    private boolean rightNavigating;
    private boolean middleNavigating;
    private boolean leftNavigating;
    private final GameInputTracker gameInput = new GameInputTracker();
    private boolean viewportFocused;
    private AssetCatalog assetCatalog;
    private Path assetDirectory = Path.of(".");
    private List<AssetCatalog.Entry> assets = List.of();
    private String assetError;
    private boolean openSceneShortcutHeld;
    private boolean newSceneShortcutHeld;
    private boolean playShortcutHeld;
    private boolean pauseShortcutHeld;

    public EditorShell(LwjglWindow window, OpenGlRenderer renderer) {
        Objects.requireNonNull(window, "window");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        projects = new EditorProjects();
        boolean contextCreated = false;
        boolean glfwAttempted = false;
        boolean gl3Attempted = false;
        try {
            ImGui.createContext();
            contextCreated = true;
            ImGui.getIO().setIniFilename(null);
            glfwAttempted = true;
            if (!glfw.init(window.handle(), true))
                throw new IllegalStateException("Unable to initialize the ImGui GLFW adapter");
            gl3Attempted = true;
            gl3.init("#version 410");
        } catch (RuntimeException | Error failure) {
            cleanupAfterFailure(projects::close, failure);
            if (gl3Attempted) cleanupAfterFailure(gl3::dispose, failure);
            if (glfwAttempted) cleanupAfterFailure(glfw::dispose, failure);
            if (contextCreated) cleanupAfterFailure(ImGui::destroyContext, failure);
            throw failure;
        }
    }

    private static void cleanupAfterFailure(Runnable cleanup, Throwable failure) {
        try { cleanup.run(); }
        catch (RuntimeException | Error exception) { failure.addSuppressed(exception); }
    }

    @Override
    public void update(double deltaSeconds) {
        projects.update(deltaSeconds);
    }

    @Override
    public boolean canClose() {
        if (!projects.dirty() || projects.quitRequested())
            return true;
        projects.requestQuit();
        return false;
    }

    public void render(LwjglWindow window) {
        ProjectSession session = projects.activeSession().orElse(null);
        if (session != displayedSession)
            resetSessionView(session);
        AetherEngine engine = session == null ? null : session.engine();
        World world = session == null ? emptyWorld : session.world();
        if (projects.quitRequested())
            window.requestClose();
        glfw.newFrame();
        ImGui.newFrame();
        handleOpenSceneShortcut(window);
        if (engine != null)
            handlePlayPauseShortcuts(window, engine);
        drawMenu(engine);
        drawProjectDialogs();
        if (session != null) {
            handleGameInput(window, engine);
            drawHierarchy(world);
            drawInspector(world);
            drawViewport(window, world);
        } else {
            ImGui.setNextWindowPos(306, 28, ImGuiCond.Always);
            ImGui.setNextWindowSize(920, 798, ImGuiCond.Always);
            ImGui.begin("Scene View", editorPanelFlags());
            ImGui.text("No project open. Use File > Open Project or New Scene.");
            ImGui.end();
        }
        drawAssets();
        drawConsole(world);
        ImGui.render();
        gl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawMenu(AetherEngine engine) {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("New Project...")) showNewProject = true;
                if (ImGui.menuItem("Open Project...", "Ctrl+O"))
                    showOpenProject = true;
                if (ImGui.beginMenu("Recent Projects")) {
                    if (projects.recentProjects().isEmpty()) ImGui.textDisabled("No recent projects");
                    for (Path recent : projects.recentProjects())
                        if (ImGui.menuItem(recent.toString())) projects.requestOpen(recent);
                    ImGui.endMenu();
                }
                if (ImGui.menuItem("Close Project", null, false, projects.activeSession().isPresent()))
                    projects.requestClose();
                if (ImGui.menuItem("Reload Project", null, false,
                        projects.activeSession().flatMap(ProjectSession::project).isPresent()))
                    projects.requestReload();
                ImGui.separator();
                if (ImGui.menuItem("New Scene", "Ctrl+N"))
                    projects.requestNewScene();
                ImGui.menuItem("Open Scene...", null, false, false);
                ImGui.menuItem("Save Scene", "Ctrl+S", false, false);
                ImGui.separator();
                if (ImGui.menuItem("Quit"))
                    projects.requestQuit();
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Game")) {
                boolean running = engine != null && engine.state() == AetherEngine.State.RUNNING;
                boolean paused = engine != null && engine.state() == AetherEngine.State.PAUSED;
                if (ImGui.menuItem(paused ? "Resume" : "Play (running)", "F5", running, paused))
                    engine.resume();
                if (ImGui.menuItem("Pause", "F6", false, running))
                    engine.pause();
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("View")) {
                showDemo = ImGui.menuItem("Demo Guide", null, showDemo);
                ImGui.menuItem("Scene Hierarchy", null, true, false);
                ImGui.menuItem("Inspector", null, true, false);
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Help")) {
                ImGui.menuItem("Aether Engine 0.1.0");
                ImGui.menuItem("Keyboard Shortcuts");
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }

    private void resetSessionView(ProjectSession session) {
        displayedSession = session;
        selected = null;
        inspectorEntity = null;
        draggingEntity = null;
        dragAxis = null;
        dragStartPosition = null;
        dragStartScale = null;
        gameInput.reset();
        viewportFocused = false;
        hasPreviousSceneMouse = false;
        rightNavigating = middleNavigating = leftNavigating = false;
        sceneCamera = SceneCamera.defaultCamera();
        sceneViewMode = projects.presentation().viewMode() == GamePresentation.ViewMode.TWO_D
                ? SceneViewMode.TWO_D : SceneViewMode.THREE_D;
        assetCatalog = null;
        assetDirectory = Path.of(".");
        assets = List.of();
        assetError = null;
        if (session != null && session.project().isPresent()) {
            try {
                assetCatalog = new AssetCatalog(session.assetDirectory());
                refreshAssets();
            } catch (IOException failure) {
                assetError = failure.getMessage();
            }
        }
    }

    private void drawProjectDialogs() {
        if (showNewProject) {
            projectDialogError = null;
            ImGui.openPopup("New Project");
            showNewProject = false;
        }
        if (ImGui.beginPopupModal("New Project")) {
            ImGui.inputText("Name", newProjectName);
            ImGui.inputText("Project file (.aetherproj)", newProjectFile);
            ImGui.textWrapped("Choose a manifest path. Its folder and assets directory will be created.");
            if (ImGui.button("Create")) {
                try {
                    if (newProjectFile.get().isBlank())
                        throw new IllegalArgumentException("Choose a project file path");
                    projects.requestCreate(Path.of(newProjectFile.get()), newProjectName.get());
                    ImGui.closeCurrentPopup();
                } catch (IllegalArgumentException failure) {
                    projectDialogError = failure.getMessage();
                }
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel")) ImGui.closeCurrentPopup();
            if (projectDialogError != null) ImGui.textWrapped(projectDialogError);
            ImGui.endPopup();
        }
        if (showOpenProject) {
            projectDialogError = null;
            ImGui.openPopup("Open Project");
            showOpenProject = false;
        }
        if (ImGui.beginPopupModal("Open Project")) {
            ImGui.text("Enter the path to an .aetherproj file:");
            ImGui.inputText("Project file", projectPath);
            ImGui.textWrapped("Relative paths start at: " + Path.of("").toAbsolutePath());
            if (ImGui.button("Open") && !projectPath.get().isBlank()) {
                try {
                    projects.requestOpen(Path.of(projectPath.get()));
                    ImGui.closeCurrentPopup();
                } catch (IllegalArgumentException failure) {
                    projectDialogError = failure.getMessage();
                }
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel"))
                ImGui.closeCurrentPopup();
            if (projectDialogError != null)
                ImGui.textWrapped(projectDialogError);
            ImGui.endPopup();
        }
        if (projects.needsConfirmation())
            ImGui.openPopup("Unsaved scene changes");
        if (ImGui.beginPopupModal("Unsaved scene changes")) {
            ImGui.textWrapped("This scene has unsaved edits. Scene saving is not available yet.");
            ImGui.text("Discard these edits to continue?");
            if (ImGui.button("Discard and continue")) {
                projects.discardAndContinue();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("Keep editing")) {
                projects.cancel();
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }

    private void handleOpenSceneShortcut(LwjglWindow window) {
        boolean controlDown = window.isKeyDown(GLFW_KEY_LEFT_CONTROL) || window.isKeyDown(GLFW_KEY_RIGHT_CONTROL);
        boolean openShortcutDown = controlDown && window.isKeyDown(GLFW_KEY_O);
        boolean newShortcutDown = controlDown && window.isKeyDown(GLFW_KEY_N);
        if (openShortcutDown && !openSceneShortcutHeld)
            showOpenProject = true;
        if (newShortcutDown && !newSceneShortcutHeld)
            projects.requestNewScene();
        openSceneShortcutHeld = openShortcutDown;
        newSceneShortcutHeld = newShortcutDown;
    }

    private void handlePlayPauseShortcuts(LwjglWindow window, AetherEngine engine) {
        boolean playShortcutDown = window.isKeyDown(GLFW_KEY_F5);
        boolean pauseShortcutDown = window.isKeyDown(GLFW_KEY_F6);
        if (playShortcutDown && !playShortcutHeld && engine.state() == AetherEngine.State.PAUSED)
            engine.resume();
        if (pauseShortcutDown && !pauseShortcutHeld && engine.state() == AetherEngine.State.RUNNING)
            engine.pause();
        playShortcutHeld = playShortcutDown;
        pauseShortcutHeld = pauseShortcutDown;
    }

    private void handleGameInput(LwjglWindow window, AetherEngine engine) {
        Set<GameKey> held = EnumSet.noneOf(GameKey.class);
        for (GameKey key : GameKey.values())
            if (window.isKeyDown(LwjglGameKeys.code(key)))
                held.add(key);
        if (window.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) held.add(GameKey.SHIFT);
        if (window.isKeyDown(GLFW_KEY_RIGHT_CONTROL)) held.add(GameKey.CONTROL);
        if (!viewportFocused || engine.state() != AetherEngine.State.RUNNING
                || ImGui.getIO().getWantTextInput() || window.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)
                || (held.contains(GameKey.CONTROL) && (held.contains(GameKey.O) || held.contains(GameKey.N)))
                || ImGui.isPopupOpen("Open Project") || ImGui.isPopupOpen("New Project")
                || ImGui.isPopupOpen("Unsaved scene changes")) {
            gameInput.suppress(held);
            projects.input(dev.aether.game.GameInput.empty());
        } else {
            projects.input(gameInput.poll(held));
        }
    }

    private void drawHierarchy(World world) {
        ImGui.setNextWindowPos(8, 28, ImGuiCond.Always);
        ImGui.setNextWindowSize(290, 480, ImGuiCond.Always);
        ImGui.begin("Scene Hierarchy", editorPanelFlags());
        if (ImGui.button("+ Create Cube")) {
            Entity entity = world.create("User Cube");
            world.add(entity, Transform.identity());
            world.add(entity, new MeshRenderer("primitive/cube", "pbr/metallic-blue"));
            selected = entity;
            projects.markDirty();
        }
        ImGui.sameLine();
        if (selected != null && ImGui.button("Delete") && world.exists(selected)) {
            world.destroy(selected);
            projects.markDirty();
            selected = null;
        }
        ImGui.separator();
        for (Entity entity : world.entities()) {
            boolean active = entity.equals(selected);
            if (ImGui.selectable(world.nameOf(entity), active, ImGuiTreeNodeFlags.SpanAvailWidth))
                selected = entity;
        }
        ImGui.end();
    }

    private void drawInspector(World world) {
        ImGui.setNextWindowPos(8, 516, ImGuiCond.Always);
        ImGui.setNextWindowSize(290, 310, ImGuiCond.Always);
        ImGui.begin("Inspector", editorPanelFlags());
        if (selected == null || !world.exists(selected)) {
            ImGui.textDisabled("Select an object in Scene View or Scene Hierarchy.");
        } else {
            syncInspectorFields(world);
            ImGui.textDisabled("Type: " + objectType(world));
            if (ImGui.inputText("Name", inspectorName) && !inspectorName.get().isBlank()) {
                world.rename(selected, inspectorName.get());
                projects.markDirty();
            }
            if (ImGui.inputText("Tags (comma separated)", inspectorTags)) {
                world.add(selected, new Tags(parseTags(inspectorTags.get())));
                projects.markDirty();
            }
            ImGui.separator();
            if (ImGui.button("Move"))
                transformMode = TransformMode.TRANSLATE;
            ImGui.sameLine();
            if (ImGui.button("Scale"))
                transformMode = TransformMode.SCALE;
            ImGui.sameLine();
            ImGui.textDisabled("Active: " + transformMode.label);
            world.get(selected, Transform.class).ifPresent(transform -> {
                float[] position = { (float) transform.position().x(), (float) transform.position().y(),
                        (float) transform.position().z() };
                if (ImGui.dragFloat3("Position", position, .05f)) {
                    world.add(selected,
                            transform.withPosition(new dev.aether.math.Vec3(position[0], position[1], position[2])));
                    projects.markDirty();
                }
                float[] scale = { (float) transform.scale().x(), (float) transform.scale().y(),
                        (float) transform.scale().z() };
                if (ImGui.dragFloat3("Scale", scale, .02f, .05f, 100f)) {
                    world.add(selected, transform.withScale(new Vec3(scale[0], scale[1], scale[2])));
                    projects.markDirty();
                }
                ImGui.text("Transform Gizmo: " + transformMode.label);
            });
            world.get(selected, MeshRenderer.class).ifPresent(mesh -> {
                ImGui.separator();
                ImGui.text("Mesh: " + mesh.meshId());
                ImGui.text("Material: " + mesh.materialId());
            });
            ImGui.separator();
            ScriptComponent script = world.get(selected, ScriptComponent.class).orElse(null);
            if (script == null) {
                if (ImGui.button("Add Script Component")) {
                    world.add(selected, new ScriptComponent(new EditorScript()));
                    projects.markDirty();
                }
            } else
                ImGui.text("Script: " + script.script().getClass().getSimpleName());
            ImGui.textDisabled("Components: "
                    + world.componentsOf(selected).stream().map(component -> component.getClass().getSimpleName())
                            .reduce((a, b) -> a + ", " + b).orElse("None"));
        }
        ImGui.end();
    }

    private void syncInspectorFields(World world) {
        if (selected.equals(inspectorEntity))
            return;
        inspectorEntity = selected;
        inspectorName.set(world.nameOf(selected));
        inspectorTags.set(String.join(", ", world.get(selected, Tags.class).orElse(Tags.none()).values()));
    }

    private String objectType(World world) {
        if (world.get(selected, MeshRenderer.class).isPresent())
            return "Mesh Object";
        if (world.get(selected, ScriptComponent.class).isPresent())
            return "Scripted Entity";
        return "Entity";
    }

    private static Set<String> parseTags(String source) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Arrays.stream(source.split(",")).map(String::trim).filter(value -> !value.isEmpty()).forEach(tags::add);
        return tags;
    }

    private static final class EditorScript implements Script {
    }

    private void drawViewport(LwjglWindow window, World world) {
        ImGui.setNextWindowPos(306, 28, ImGuiCond.Always);
        ImGui.setNextWindowSize(920, 798, ImGuiCond.Always);
        ImGui.begin("Scene View", ImGuiWindowFlags.NoScrollbar | editorPanelFlags());
        viewportFocused = ImGui.isWindowFocused();
        if (ImGui.button("3D", sceneViewMode == SceneViewMode.THREE_D ? 58 : 48, 0))
            sceneViewMode = SceneViewMode.THREE_D;
        ImGui.sameLine();
        if (ImGui.button("2D", sceneViewMode == SceneViewMode.TWO_D ? 58 : 48, 0))
            sceneViewMode = SceneViewMode.TWO_D;
        ImGui.sameLine();
        ImGui.textDisabled(sceneViewMode == SceneViewMode.THREE_D ? "Perspective" : "Orthographic");
        ImGui.separator();
        float viewportWidth = ImGui.getContentRegionAvailX(), viewportHeight = ImGui.getContentRegionAvailY() - 28;
        Matrix4f projection = projection(viewportWidth, viewportHeight);
        int texture = renderer.renderScene(world, Math.max(1, (int) viewportWidth), Math.max(1, (int) viewportHeight),
                projection, viewMatrix());
        ImGui.image(texture, viewportWidth, viewportHeight, 0, 1, 1, 0);
        float left = ImGui.getItemRectMinX(), top = ImGui.getItemRectMinY();
        boolean hovered = ImGui.isItemHovered();
        navigateViewport(window, hovered);
        if (hovered && ImGui.isMouseClicked(0) && !window.isKeyDown(GLFW_KEY_LEFT_ALT)
                && !window.isKeyDown(GLFW_KEY_RIGHT_ALT)) {
            Entity hit = pick(world, left, top, viewportWidth, viewportHeight, ImGui.getMousePosX(),
                    ImGui.getMousePosY());
            if (hit != null)
                selected = hit;
        }
        drawGizmo(world, left, top, viewportWidth, viewportHeight);
        GamePresentation presentation = projects.presentation();
        drawGameHud(left, top, viewportWidth, viewportHeight, presentation);
        if (!presentation.controls().isBlank())
            ImGui.textColored(.35f, .9f, 1f, 1f, presentation.controls());
        else
            ImGui.textColored(.35f, .75f, 1f, 1f,
                    sceneViewMode == SceneViewMode.THREE_D
                            ? "Navigate: RMB + WASD/QE fly | Alt + LMB orbit | MMB pan | wheel zoom"
                            : "2D orthographic view: MMB pan | wheel zoom");
        ImGui.end();
    }

    private void drawGameHud(float left, float top, float width, float height, GamePresentation presentation) {
        ImDrawList draw = ImGui.getWindowDrawList();
        for (var label : presentation.labels())
            draw.addText(left + width * (float) label.x(), top + height * (float) label.y(),
                    Integer.reverseBytes(label.rgba()), label.text());
    }

    private void navigateViewport(LwjglWindow window, boolean hovered) {
        LwjglWindow.CursorPosition mouse = window.cursorPosition();
        double dx = hasPreviousSceneMouse ? mouse.x() - previousSceneMouseX : 0,
                dy = hasPreviousSceneMouse ? mouse.y() - previousSceneMouseY : 0;
        previousSceneMouseX = mouse.x();
        previousSceneMouseY = mouse.y();
        hasPreviousSceneMouse = true;
        boolean right = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT),
                middle = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE),
                left = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT),
                alt = window.isKeyDown(GLFW_KEY_LEFT_ALT) || window.isKeyDown(GLFW_KEY_RIGHT_ALT);
        if (!right)
            rightNavigating = false;
        if (!middle)
            middleNavigating = false;
        if (!left)
            leftNavigating = false;
        if (hovered && right)
            rightNavigating = true;
        if (hovered && middle)
            middleNavigating = true;
        if (hovered && left && alt)
            leftNavigating = true;
        if (sceneViewMode == SceneViewMode.TWO_D) {
            if (middleNavigating)
                sceneCamera.panOrthographic(-dx * .02, dy * .02);
        } else {
            if (rightNavigating) {
                sceneCamera.orbit(-dx * .01, dy * .01);
                double speed = window.isKeyDown(GLFW_KEY_LEFT_SHIFT) || window.isKeyDown(GLFW_KEY_RIGHT_SHIFT) ? .25
                        : .08;
                if (window.isKeyDown(GLFW_KEY_W))
                    sceneCamera.fly(0, 0, speed);
                if (window.isKeyDown(GLFW_KEY_S))
                    sceneCamera.fly(0, 0, -speed);
                if (window.isKeyDown(GLFW_KEY_A))
                    sceneCamera.fly(-speed, 0, 0);
                if (window.isKeyDown(GLFW_KEY_D))
                    sceneCamera.fly(speed, 0, 0);
                if (window.isKeyDown(GLFW_KEY_Q))
                    sceneCamera.fly(0, -speed, 0);
                if (window.isKeyDown(GLFW_KEY_E))
                    sceneCamera.fly(0, speed, 0);
            }
            if (leftNavigating)
                sceneCamera.orbit(-dx * .01, dy * .01);
            if (middleNavigating)
                sceneCamera.pan(-dx * .02, dy * .02);
        }
        float wheel = ImGui.getIO().getMouseWheel();
        if (hovered && wheel != 0)
            sceneCamera.dolly(wheel * Math.max(.1, distanceToFocus() * .12));
    }

    private double distanceToFocus() {
        Vec3 p = sceneCamera.position(), t = sceneCamera.target();
        double x = p.x() - t.x(), y = p.y() - t.y(), z = p.z() - t.z();
        return Math.sqrt(x * x + y * y + z * z);
    }

    private Entity pick(World world, float left, float top, float width, float height, float mouseX, float mouseY) {
        Entity nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Entity entity : world.view(MeshRenderer.class).keySet()) {
            Transform transform = world.get(entity, Transform.class).orElse(null);
            if (transform == null)
                continue;
            float[] point = project(transform.position(), left, top, width, height);
            float distance = (point[0] - mouseX) * (point[0] - mouseX) + (point[1] - mouseY) * (point[1] - mouseY);
            if (distance < 50 * 50 && distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void drawGizmo(World world, float left, float top, float width, float height) {
        if (draggingEntity != null && !world.exists(draggingEntity)) {
            draggingEntity = null;
            dragAxis = null;
        }
        if (selected == null || !world.exists(selected))
            return;
        Transform transform = world.get(selected, Transform.class).orElse(null);
        if (transform == null)
            return;
        float[] point = project(transform.position(), left, top, width, height);
        float x = point[0], y = point[1];
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRect(x - 26, y - 26, x + 26, y + 26, 0xFFFFFFFF, 0, 0, 2);
        draw.addLine(x, y, x + 54, y, 0xFF3030FF, 3);
        draw.addLine(x, y, x, y - 54, 0xFF30FF30, 3);
        draw.addText(x + 57, y - 8, 0xFF3030FF, transformMode == TransformMode.TRANSLATE ? "X" : "Scale X");
        draw.addText(x - 7, y - 70, 0xFF30FF30, transformMode == TransformMode.TRANSLATE ? "Y" : "Scale Y");
        if (ImGui.isMouseClicked(0)) {
            float mouseX = ImGui.getMousePosX(), mouseY = ImGui.getMousePosY();
            if (Math.abs(mouseY - y) < 14 && mouseX >= x && mouseX <= x + 64)
                beginDrag(Axis.X, transform);
            else if (Math.abs(mouseX - x) < 14 && mouseY <= y && mouseY >= y - 64)
                beginDrag(Axis.Y, transform);
        }
        if (draggingEntity != null) {
            if (ImGui.isMouseDown(0)) {
                double delta = dragAxis == Axis.X ? (ImGui.getMousePosX() - dragStartMouseX) / 50d
                        : (dragStartMouseY - ImGui.getMousePosY()) / 50d;
                if (delta != 0)
                    projects.markDirty();
                Vec3 start = dragStartPosition;
                if (transformMode == TransformMode.TRANSLATE) {
                    Vec3 position = dragAxis == Axis.X ? new Vec3(start.x() + delta, start.y(), start.z())
                            : new Vec3(start.x(), start.y() + delta, start.z());
                    world.add(draggingEntity, transform.withPosition(position));
                } else {
                    Vec3 scale = dragAxis == Axis.X
                            ? new Vec3(Math.max(.05, dragStartScale.x() + delta), dragStartScale.y(),
                                    dragStartScale.z())
                            : new Vec3(dragStartScale.x(), Math.max(.05, dragStartScale.y() + delta),
                                    dragStartScale.z());
                    world.add(draggingEntity, transform.withScale(scale));
                }
            } else {
                draggingEntity = null;
                dragAxis = null;
            }
        }
    }

    private void beginDrag(Axis axis, Transform transform) {
        draggingEntity = selected;
        dragAxis = axis;
        dragStartPosition = transform.position();
        dragStartScale = transform.scale();
        dragStartMouseX = ImGui.getMousePosX();
        dragStartMouseY = ImGui.getMousePosY();
    }

    private Matrix4f projection(float width, float height) {
        float aspect = Math.max(1, width) / Math.max(1, height);
        if (sceneViewMode == SceneViewMode.THREE_D)
            return new Matrix4f().perspective((float) Math.toRadians(55), aspect, .1f, 100);
        float halfHeight = (float) (sceneCamera.focusDistance() * Math.tan(Math.toRadians(55) / 2));
        return new Matrix4f().ortho(-halfHeight * aspect, halfHeight * aspect, -halfHeight, halfHeight, .1f, 100);
    }

    private Matrix4f viewMatrix() {
        return sceneViewMode == SceneViewMode.THREE_D ? sceneCamera.viewMatrix() : sceneCamera.orthographicViewMatrix();
    }

    private float[] project(Vec3 position, float left, float top, float width, float height) {
        Vector4f clip = projection(width, height).mul(viewMatrix())
                .transform(new Vector4f((float) position.x(), (float) position.y(), (float) position.z(), 1));
        float ndcX = clip.x / clip.w, ndcY = clip.y / clip.w;
        return new float[] { left + (ndcX * .5f + .5f) * width, top + (1 - (ndcY * .5f + .5f)) * height };
    }

    private enum Axis {
        X, Y
    }

    private enum SceneViewMode {
        THREE_D, TWO_D
    }

    private enum TransformMode {
        TRANSLATE("Move"), SCALE("Scale");

        private final String label;

        TransformMode(String label) {
            this.label = label;
        }
    }

    private void drawAssets() {
        ImGui.setNextWindowPos(1234, 28, ImGuiCond.Always);
        ImGui.setNextWindowSize(300, 390, ImGuiCond.Always);
        ImGui.begin("Asset Browser", editorPanelFlags());
        if (assetCatalog == null) {
            ImGui.textWrapped(assetError == null ? "Open a project to browse its assets." : assetError);
        } else {
            ImGui.textWrapped("Assets / " + assetDirectory);
            if (ImGui.button("Refresh")) refreshAssets();
            if (!assetDirectory.normalize().toString().isEmpty() && ImGui.button("..")) {
                Path parent = assetDirectory.getParent();
                assetDirectory = parent == null ? Path.of(".") : parent;
                refreshAssets();
            }
            for (var asset : assets) {
                if (asset.directory()) {
                    if (ImGui.selectable("[dir] " + asset.path().getFileName())) {
                        assetDirectory = asset.path();
                        refreshAssets();
                    }
                } else {
                    ImGui.bulletText(asset.path().getFileName().toString());
                }
            }
            if (assetError != null) ImGui.textWrapped(assetError);
        }
        ImGui.end();
    }

    private void refreshAssets() {
        try {
            assets = assetCatalog.list(assetDirectory);
            assetError = null;
        } catch (IOException | IllegalArgumentException failure) {
            assets = List.of();
            assetError = failure.getMessage();
        }
    }

    private void drawConsole(World world) {
        ImGui.setNextWindowPos(1234, 426, ImGuiCond.Always);
        ImGui.setNextWindowSize(300, 400, ImGuiCond.Always);
        ImGui.begin("Console", editorPanelFlags());
        ImGui.textColored(.3f, 1f, .55f, 1f, "[Aether] Editor ready");
        ImGui.textWrapped(projects.activeSession().flatMap(ProjectSession::project)
                .map(project -> "Project: " + project.config().name())
                .orElse(projects.activeSession().isPresent() ? "Untitled scene" : "No project open"));
        ImGui.text("Scene: " + world.entityCount() + " entities" + (projects.dirty() ? " *" : ""));
        projects.error().ifPresent(error -> ImGui.textWrapped("[Project] " + error));
        ImGui.text("Physics + script systems enabled");
        ImGui.end();
    }

    private int editorPanelFlags() {
        return ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;
    }

    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        try {
            projects.close();
        } finally {
            emptyWorld.close();
            try {
                gl3.dispose();
            } finally {
                try { glfw.dispose(); }
                finally { ImGui.destroyContext(); }
            }
        }
    }
}
