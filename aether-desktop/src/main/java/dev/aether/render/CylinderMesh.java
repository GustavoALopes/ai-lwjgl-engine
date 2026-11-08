package dev.aether.render;

/** CPU-only upright cylinder: radius one, Y from -1 to 1, XYZ/UV vertices. */
final class CylinderMesh {
    private CylinderMesh() { }

    static float[] vertices(int segments) {
        if (segments < 3 || segments > 4096)
            throw new IllegalArgumentException("segments must be between 3 and 4096");
        float[] result = new float[segments * 12 * 5];
        int offset = 0;
        for (int i = 0; i < segments; i++) {
            double a = 2 * Math.PI * i / segments;
            double b = 2 * Math.PI * (i + 1) / segments;
            float x0 = (float) Math.cos(a), z0 = (float) Math.sin(a);
            float x1 = (float) Math.cos(b), z1 = (float) Math.sin(b);
            float u0 = (float) i / segments, u1 = (float) (i + 1) / segments;
            float[][] triangleVertices = {
                    {x0, -1, z0, u0, 0}, {x0, 1, z0, u0, 1}, {x1, 1, z1, u1, 1},
                    {x0, -1, z0, u0, 0}, {x1, 1, z1, u1, 1}, {x1, -1, z1, u1, 0},
                    {0, 1, 0, .5f, .5f}, {x1, 1, z1, (x1 + 1) / 2, (z1 + 1) / 2},
                    {x0, 1, z0, (x0 + 1) / 2, (z0 + 1) / 2},
                    {0, -1, 0, .5f, .5f}, {x0, -1, z0, (x0 + 1) / 2, (z0 + 1) / 2},
                    {x1, -1, z1, (x1 + 1) / 2, (z1 + 1) / 2}
            };
            for (float[] vertex : triangleVertices) {
                System.arraycopy(vertex, 0, result, offset, 5);
                offset += 5;
            }
        }
        return result;
    }
}
