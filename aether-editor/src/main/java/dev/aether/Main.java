package dev.aether;

import dev.aether.platform.DesktopRuntime;
import dev.aether.editor.EditorShell;
import dev.aether.platform.WindowConfig;
import dev.aether.render.OpenGlRenderer;
import dev.aether.render.RenderSettings;
import dev.aether.editor.EditorProjects;
import dev.aether.game.GameInput;
import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--headless")) {
            if (args.length > 2)
                throw new IllegalArgumentException("Usage: --headless [project.aetherproj]");
            try (var projects = new EditorProjects()) {
                if (args.length == 2)
                    projects.requestOpen(Path.of(args[1]));
                projects.update(0);
                projects.input(GameInput.empty());
                projects.presentation();
                projects.error().ifPresent(error -> { throw new IllegalStateException(error); });
                if (args.length == 2) {
                    var session = projects.activeSession().orElseThrow();
                    System.out.println("Loaded " + session.project().orElseThrow().config().name()
                            + ": " + session.world().entityCount() + " entities");
                }
            }
            return;
        }

        DesktopRuntime.run(
            WindowConfig.editorDefaults(),
            new OpenGlRenderer(RenderSettings.productionDefaults()),
            EditorShell::new
        );
    }
}
