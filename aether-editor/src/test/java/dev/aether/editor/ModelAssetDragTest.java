package dev.aether.editor;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelAssetDragTest {
    @Test void holdsStablePayloadAndDeliversOnlyOncePerGesture() {
        try (var projects = new EditorProjects()) {
            var session = projects.activeSession().orElseThrow();
            var drag = new ModelAssetDrag();
            Path path = Path.of("models/table.obj");
            var payload = drag.offer(session, path);
            assertSame(payload, drag.offer(session, path));
            assertTrue(drag.active());
            assertEquals(path, drag.consume(payload, session));
            assertNull(drag.consume(payload, session));
            assertFalse(drag.active());
            assertNotSame(payload, drag.offer(session, path));
        }
    }

    @Test void cancelledOrStaleProjectPayloadsCannotImport() {
        try (var projects = new EditorProjects()) {
            var session = projects.activeSession().orElseThrow();
            var drag = new ModelAssetDrag();
            var payload = drag.offer(session, Path.of("table.obj"));
            projects.requestNewScene();
            projects.update(0);
            assertNull(drag.consume(payload, projects.activeSession().orElseThrow()));
            drag.cancel();
            assertNull(drag.consume(payload, session));
            assertFalse(drag.active());
        }
    }
}
