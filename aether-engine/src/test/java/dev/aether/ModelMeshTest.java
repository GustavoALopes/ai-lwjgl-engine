package dev.aether;

import dev.aether.math.Vec3;
import dev.aether.render.ModelMesh;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelMeshTest {
    @Test void snapshotsGeometryAtBothBoundaries() {
        float[] vertices = new float[24];
        vertices[1] = -2;
        var part = new ModelMesh.Part(vertices, new Vec3(1, 1, 1));
        var parts = new ArrayList<>(List.of(part));
        var mesh = new ModelMesh(parts);
        vertices[1] = 5;
        part.vertices()[1] = 10;
        parts.clear();
        assertEquals(-2, mesh.minimumY());
        assertEquals(3, part.vertexCount());
        assertThrows(UnsupportedOperationException.class, () -> mesh.parts().clear());
    }

    @Test void rejectsEmptyIncompleteAndNonFiniteGeometry() {
        assertThrows(IllegalArgumentException.class, () -> new ModelMesh(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ModelMesh.Part(new float[8], Vec3.ZERO));
        float[] values = new float[24];
        values[0] = Float.NaN;
        assertThrows(IllegalArgumentException.class, () -> new ModelMesh.Part(values, Vec3.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelMesh.Part(new float[24], new Vec3(Double.NaN, 0, 0)));
    }
}
