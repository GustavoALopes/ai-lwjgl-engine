package dev.aether.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ObjModelLoaderTest {
    @TempDir Path directory;

    @Test void triangulatesNegativeIndicesAndLoadsSiblingMaterialAndNormalsWithoutGl() throws Exception {
        Files.writeString(directory.resolve("table.mtl"), "newmtl metal\nKd 0.2 0.4 0.6\n");
        Path file = directory.resolve("table.OBJ");
        Files.writeString(file, """
                mtllib table.mtl
                v -1 -2 0
                v 1 -2 0
                v 1 2 0
                v -1 2 0
                vt 0 0
                vt 1 0
                vt 1 1
                vt 0 1
                usemtl metal
                f -4/1 -3/2 -2/3 -1/4
                """);
        var model = ObjModelLoader.load(file);
        for (int i = 0; i < 100; i++) {
            var repeated = ObjModelLoader.load(file);
            assertEquals(6, repeated.parts().stream().mapToInt(ModelMesh.Part::vertexCount).sum());
        }
        assertEquals(6, model.parts().stream().mapToInt(ModelMesh.Part::vertexCount).sum());
        assertEquals(-2, model.minimumY());
        var part = model.parts().getFirst();
        assertEquals(.2, part.color().x(), 1e-5);
        assertEquals(.6, part.color().z(), 1e-5);
        assertEquals(1, Math.abs(part.vertices()[7]), 1e-5);
    }

    @Test void preservesSeparateMaterialParts() throws Exception {
        Files.writeString(directory.resolve("colors.mtl"), "newmtl red\nKd 1 0 0\nnewmtl blue\nKd 0 0 1\n");
        Path file = directory.resolve("parts.obj");
        Files.writeString(file, "mtllib colors.mtl\nv 0 0 0\nv 1 0 0\nv 0 1 0\nusemtl red\nf 1 2 3\nusemtl blue\nf 3 2 1\n");
        var model = ObjModelLoader.load(file);
        assertEquals(2, model.parts().size());
        assertTrue(model.parts().stream().anyMatch(p -> p.color().x() == 1));
        assertTrue(model.parts().stream().anyMatch(p -> p.color().z() == 1));
    }

    @Test void rejectsMissingMalformedAndTriangleFreeFiles() throws Exception {
        assertThrows(IOException.class, () -> ObjModelLoader.load(directory.resolve("missing.obj")));
        assertThrows(IllegalArgumentException.class, () -> ObjModelLoader.load(directory.resolve("file.fbx")));
        Path file = directory.resolve("invalid.obj");
        Files.writeString(file, "v 0 0 0\nf 1 2 99\n");
        assertThrows(IOException.class, () -> ObjModelLoader.load(file));
        Files.writeString(file, "v 0 0 0\n");
        assertThrows(IOException.class, () -> ObjModelLoader.load(file));
    }

    @Test void preservesExplicitNormalsAndGeneratesFiniteNormalsForDegenerateFaces() throws Exception {
        Path file = directory.resolve("normals.obj");
        Files.writeString(file, "v 0 0 0\nv 1 0 0\nv 0 1 0\nvn 0 1 0\nf 1//1 2//1 3//1\n");
        float[] vertices = ObjModelLoader.load(file).parts().getFirst().vertices();
        assertEquals(1, vertices[6]);
        assertEquals(0, vertices[7]);
        Files.writeString(file, "v 0 0 0\nv 1 0 0\nv 2 0 0\nf 1 2 3\n");
        vertices = ObjModelLoader.load(file).parts().getFirst().vertices();
        assertEquals(1, vertices[6]);
        for (float value : vertices) assertTrue(Float.isFinite(value));
    }
}
