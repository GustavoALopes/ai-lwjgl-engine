package dev.aether.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CylinderMeshTest {
    @Test void generatesClosedCylinderWithOutwardTrianglesAndValidUvs() {
        float[] vertices = CylinderMesh.vertices(32);
        assertEquals(32 * 12 * 5, vertices.length);
        for (int i = 0; i < vertices.length; i += 5) {
            double radiusSquared = vertices[i] * vertices[i] + vertices[i + 2] * vertices[i + 2];
            assertTrue(radiusSquared <= 1.000001);
            assertTrue(vertices[i + 1] == -1 || vertices[i + 1] == 1);
            assertTrue(vertices[i + 3] >= 0 && vertices[i + 3] <= 1);
            assertTrue(vertices[i + 4] >= 0 && vertices[i + 4] <= 1);
        }
        for (int i = 0; i < vertices.length; i += 15) {
            double ax = vertices[i + 5] - vertices[i], ay = vertices[i + 6] - vertices[i + 1],
                    az = vertices[i + 7] - vertices[i + 2];
            double bx = vertices[i + 10] - vertices[i], by = vertices[i + 11] - vertices[i + 1],
                    bz = vertices[i + 12] - vertices[i + 2];
            double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            double cx = (vertices[i] + vertices[i + 5] + vertices[i + 10]) / 3.0;
            double cy = (vertices[i + 1] + vertices[i + 6] + vertices[i + 11]) / 3.0;
            double cz = (vertices[i + 2] + vertices[i + 7] + vertices[i + 12]) / 3.0;
            assertTrue(nx * cx + ny * cy + nz * cz > 0, "Triangle must face outward");
        }
        // First/last side edges meet, making a closed seam.
        int lastSegment = 31 * 12 * 5;
        assertEquals(vertices[0], vertices[lastSegment + 25], 1e-6);
        assertEquals(vertices[2], vertices[lastSegment + 27], 1e-6);
    }

    @Test void validatesResolutionAndReturnsIndependentArrays() {
        assertThrows(IllegalArgumentException.class, () -> CylinderMesh.vertices(2));
        assertThrows(IllegalArgumentException.class, () -> CylinderMesh.vertices(4097));
        float[] first = CylinderMesh.vertices(3);
        first[0] = 99;
        assertEquals(1, CylinderMesh.vertices(3)[0]);
    }
}
