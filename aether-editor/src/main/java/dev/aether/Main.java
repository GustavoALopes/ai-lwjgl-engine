package dev.aether;

import dev.aether.platform.DesktopRuntime;
import dev.aether.editor.EditorShell;
import dev.aether.platform.WindowConfig;
import dev.aether.render.OpenGlRenderer;
import dev.aether.render.RenderSettings;

public final class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--headless")) {
            try (var e = Aether.createEngine()) {
                e.start();
                e.tick(0);
                e.stop();
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
