package dev.aether.project;

import dev.aether.game.GameModule;
import java.io.IOException;
import java.net.URLClassLoader;

/** Loads only the configured entry point from a project JAR; each session owns its loader. */
public final class GameModuleLoader implements ProjectInitializer {
    @Override
    public void initialize(ProjectSession session) throws IOException {
        var project = session.project().orElseThrow();
        if (project.startScene().isPresent())
            throw new IOException("Serialized scene loading is not configured for this host");
        if (project.config().gameModule().isEmpty())
            return;
        var config = project.config().gameModule().orElseThrow();
        var jar = project.gameModuleJar().orElseThrow();
        var loader = session.own(new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},
                GameModule.class.getClassLoader()));
        try {
            Class<?> type = Class.forName(config.entryPoint(), false, loader);
            if (type.getClassLoader() != loader || !GameModule.class.isAssignableFrom(type))
                throw new IOException("Entry point must implement GameModule and load through the project JAR: " + jar);
            var module = (GameModule) type.getConstructor().newInstance();
            session.installModule(module);
            module.initialize(session);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IOException("Unable to load game module " + config.entryPoint() + " from " + jar, exception);
        }
    }
}
