package dev.aether.render;

import dev.aether.ecs.Component;
import dev.aether.math.Vec3;
import java.util.List;
import java.util.Objects;

/** Immutable CPU geometry. No native or graphics resources are owned by a world. */
public record ModelMesh(List<Part> parts) implements Component {
    public ModelMesh {
        parts = List.copyOf(parts);
        if (parts.isEmpty()) throw new IllegalArgumentException("A model must contain triangles");
    }

    /** Interleaved position XYZ, UV, normal XYZ, expanded into triangles. */
    public record Part(float[] vertices, Vec3 color) {
        public Part {
            Objects.requireNonNull(color, "color");
            if (!Double.isFinite(color.x()) || !Double.isFinite(color.y()) || !Double.isFinite(color.z()))
                throw new IllegalArgumentException("Non-finite material color");
            vertices = vertices.clone();
            if (vertices.length == 0 || vertices.length % 24 != 0)
                throw new IllegalArgumentException("Expected complete triangles with eight floats per vertex");
            for (int i = 0; i < vertices.length; i++)
                if (!Float.isFinite(vertices[i]))
                    throw new IllegalArgumentException("Non-finite vertex value at float " + i);
        }
        @Override public float[] vertices() { return vertices.clone(); }
        public int vertexCount() { return vertices.length / 8; }
    }

    public double minimumY() {
        double result = Double.POSITIVE_INFINITY;
        for (Part part : parts)
            for (int i = 1; i < part.vertices.length; i += 8) result = Math.min(result, part.vertices[i]);
        return result;
    }
}
