package dev.aether.project;

import dev.aether.game.GameInput;
import dev.aether.game.GameKey;
import dev.aether.game.GameModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class GameModuleLoaderTest {
    @TempDir Path temporary;
    private static final String MODULE = """
            package modulefixture;
            import dev.aether.game.*;
            import dev.aether.script.*;
            import java.nio.file.*;
            public final class Entry implements GameModule {
                private static int launches;
                private GameContext context;
                private int ticks;
                public void initialize(GameContext context) {
                    this.context = context;
                    context.world().create("launch-" + (++launches));
                    var entity = context.world().create("script");
                    context.world().add(entity, new ScriptComponent(new Script() {
                        public void onDestroy(dev.aether.ecs.World w, dev.aether.ecs.Entity e) { log("script"); }
                    }));
                    context.world().addSystem((world, dt) -> ticks++);
                    context.own(() -> log("asset"));
                }
                public void input(GameInput input) {
                    if (input.pressed().contains(GameKey.SPACE)) context.world().create("input");
                }
                public GamePresentation presentation() {
                    return new GamePresentation(GamePresentation.ViewMode.TWO_D,
                        java.util.List.of(new GamePresentation.Label(.5, .5, "ticks-" + ticks, 0xFFFFFFFF)),
                        "Space");
                }
                public void close() { log("module"); }
                private void log(String value) {
                    try {
                        Files.writeString(context.assetDirectory().resolve("events.txt"), value + "\\n",
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (java.io.IOException e) { throw new IllegalStateException(e); }
                }
            }
            """;

    @Test void loadsFromAnExternalJarAndDispatchesInputAndPresentation() throws Exception {
        Path file = project(MODULE);
        try (var manager = new ProjectManager()) {
            manager.requestOpen(file);
            manager.applyPending();
            var session = manager.activeSession().orElseThrow();
            var module = session.module().orElseThrow();
            assertNotSame(GameModule.class.getClassLoader(), module.getClass().getClassLoader());
            assertEquals("launch-1", session.world().nameOf(session.world().entities().getFirst()));
            manager.tick(0);
            module.input(new GameInput(Set.of(GameKey.SPACE), Set.of(GameKey.SPACE)));
            assertEquals(3, session.world().entityCount());
            assertEquals("ticks-1", module.presentation().labels().getFirst().text());
            assertEquals("Space", module.presentation().controls());
        }
    }

    @Test void closesScriptsThenModuleThenAssetsAndReleasesTheJar() throws Exception {
        Path file = project(MODULE);
        var manager = new ProjectManager();
        manager.requestOpen(file);
        manager.applyPending();
        manager.tick(0);
        var session = manager.activeSession().orElseThrow();
        manager.close();
        assertEquals(java.util.List.of("script", "module", "asset"),
                Files.readAllLines(file.getParent().resolve("assets/events.txt")));
        assertTrue(session.module().isEmpty());
        Path jar = file.getParent().resolve("game.jar");
        Files.move(jar, jar.resolveSibling("released.jar"));
    }

    @Test void reopeningTheSameJarUsesFreshClassStateAndWorlds() throws Exception {
        Path file = project(MODULE);
        try (var manager = new ProjectManager()) {
            ProjectSession previous = null;
            ClassLoader previousLoader = null;
            for (int index = 0; index < 3; index++) {
                manager.requestOpen(file);
                manager.applyPending();
                var session = manager.activeSession().orElseThrow();
                ClassLoader loader = session.module().orElseThrow().getClass().getClassLoader();
                assertNotSame(previousLoader, loader);
                assertEquals("launch-1", session.world().nameOf(session.world().entities().getFirst()));
                if (previous != null) assertEquals(ProjectSession.State.CLOSED, previous.state());
                previous = session;
                previousLoader = loader;
            }
        }
    }

    @Test void rejectsWrongTypeMissingConstructorAndThrowingConstructor() throws Exception {
        for (String source : java.util.List.of(
                "package modulefixture; public class Entry { }",
                "package modulefixture; public class Entry implements dev.aether.game.GameModule { "
                        + "public Entry(int value) {} public void initialize(dev.aether.game.GameContext c) {} }",
                "package modulefixture; public class Entry implements dev.aether.game.GameModule { "
                        + "public Entry() { throw new IllegalStateException(\"constructor failed\"); } "
                        + "public void initialize(dev.aether.game.GameContext c) {} }")) {
            Path file = project(source);
            try (var manager = new ProjectManager()) {
                manager.requestReset(session -> session.world().create("keep"));
                manager.applyPending();
                var previous = manager.activeSession().orElseThrow();
                manager.requestOpen(file);
                assertThrows(IOException.class, manager::applyPending);
                assertSame(previous, manager.activeSession().orElseThrow());
                Files.move(file.getParent().resolve("game.jar"), file.getParent().resolve("released.jar"));
            }
        }
    }

    @Test void failedInitializationClosesModuleAndAcquiredAssetsAndKeepsCurrentSession() throws Exception {
        String source = MODULE.replace("context.own(() -> log(\"asset\"));",
                "context.own(() -> log(\"asset\")); throw new IllegalStateException(\"init failed\");");
        Path file = project(source);
        try (var manager = new ProjectManager()) {
            manager.requestReset(session -> session.world().create("keep"));
            manager.applyPending();
            var previous = manager.activeSession().orElseThrow();
            manager.requestOpen(file);
            assertThrows(IllegalStateException.class, manager::applyPending);
            assertSame(previous, manager.activeSession().orElseThrow());
            assertEquals(java.util.List.of("module", "asset"),
                    Files.readAllLines(file.getParent().resolve("assets/events.txt")));
        }
    }

    @Test void moduleCloseFailureDoesNotSkipAssetsOrClassLoaderCleanup() throws Exception {
        Path file = project(MODULE.replace("public void close() { log(\"module\"); }",
                "public void close() { log(\"module\"); throw new IllegalStateException(\"close failed\"); }"));
        var manager = new ProjectManager();
        manager.requestOpen(file);
        manager.applyPending();
        assertThrows(RuntimeException.class, manager::close);
        assertEquals(java.util.List.of("module", "asset"),
                Files.readAllLines(file.getParent().resolve("assets/events.txt")));
        Files.move(file.getParent().resolve("game.jar"), file.getParent().resolve("released.jar"));
    }

    @Test void cannotSubstituteAParentClasspathClassForTheConfiguredJarEntryPoint() throws Exception {
        Path file = project(MODULE);
        var config = new ProjectSerializer().read(file).config();
        new ProjectSerializer().write(file, new ProjectConfig(1, config.name(), config.assetDirectory(),
                Optional.empty(), Optional.of(new GameModuleConfig(Path.of("game.jar"), GameModule.class.getName()))));
        try (var manager = new ProjectManager()) {
            manager.requestOpen(file);
            assertThrows(IOException.class, manager::applyPending);
            assertTrue(manager.activeSession().isEmpty());
        }
    }

    private Path project(String source) throws Exception {
        Path directory = Files.createTempDirectory(temporary, "project-");
        Files.createDirectory(directory.resolve("assets"));
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path java = Files.writeString(directory.resolve("Entry.java"), source);
        Path core = Path.of(GameModule.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-classpath", core.toString(), "-d", classes.toString(), java.toString()));
        try (var jar = new JarOutputStream(Files.newOutputStream(directory.resolve("game.jar")));
             var paths = Files.walk(classes)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                jar.putNextEntry(new JarEntry(classes.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, jar);
                jar.closeEntry();
            }
        }
        Path file = directory.resolve("Game.aetherproj");
        new ProjectSerializer().write(file, new ProjectConfig(1, "Fixture", Path.of("assets"),
                Optional.empty(), Optional.of(new GameModuleConfig(Path.of("game.jar"), "modulefixture.Entry"))));
        return file;
    }
}
