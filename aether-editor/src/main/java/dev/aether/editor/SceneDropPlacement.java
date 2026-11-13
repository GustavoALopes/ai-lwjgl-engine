package dev.aether.editor;

import dev.aether.ecs.World;
import dev.aether.math.Vec3;
import dev.aether.render.CutawaySurface;
import dev.aether.render.MeshRenderer;
import dev.aether.render.ModelMesh;
import dev.aether.scene.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Cursor ray against scene geometry, then the ground plane. No window/context required. */
public final class SceneDropPlacement {
    private SceneDropPlacement() { }

    public static Vec3 position(World world, Matrix4f projection, Matrix4f view,
                                float x, float y, boolean twoDimensional) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Invalid cursor");
        Matrix4f inverse = new Matrix4f(projection).mul(view).invert();
        Vector3f origin = inverse.transformProject(new Vector3f(x, y, -1));
        Vector3f direction = inverse.transformProject(new Vector3f(x, y, 1)).sub(origin).normalize();
        float closest = Float.POSITIVE_INFINITY;
        for (var entity : world.view(MeshRenderer.class).keySet()) {
            // Camera-facing walls can be visually lowered; do not place on their hidden full-height geometry.
            if (world.get(entity, CutawaySurface.class).isPresent()) continue;
            Transform transform = world.get(entity, Transform.class).orElse(null);
            if (transform == null || transform.scale().x() == 0 || transform.scale().y() == 0 || transform.scale().z() == 0)
                continue;
            Matrix4f local = new Matrix4f().translate((float) transform.position().x(), (float) transform.position().y(), (float) transform.position().z())
                    .rotateXYZ((float) Math.toRadians(transform.rotationDegrees().x()), (float) Math.toRadians(transform.rotationDegrees().y()),
                            (float) Math.toRadians(transform.rotationDegrees().z()))
                    .scale((float) transform.scale().x(), (float) transform.scale().y(), (float) transform.scale().z()).invert();
            Vector3f o = local.transformPosition(new Vector3f(origin));
            Vector3f d = local.transformDirection(new Vector3f(direction));
            ModelMesh mesh = world.get(entity, ModelMesh.class).orElse(null);
            if (mesh == null) {
                closest = Math.min(closest, box(o, d));
            } else {
                for (var part : mesh.parts()) {
                    float[] vertices = part.vertices();
                    for (int i = 0; i < vertices.length; i += 24)
                        closest = Math.min(closest, triangle(o, d, vertices, i));
                }
            }
        }
        if (!Float.isFinite(closest)) {
            float denominator = twoDimensional ? direction.z : direction.y;
            float numerator = twoDimensional ? origin.z : origin.y;
            closest = Math.abs(denominator) < 1e-6 ? 10 : -numerator / denominator;
            if (closest < 0 || !Float.isFinite(closest)) closest = 10;
        }
        Vector3f hit = new Vector3f(direction).mul(closest).add(origin);
        return new Vec3(hit.x, hit.y, hit.z);
    }

    private static float box(Vector3f o, Vector3f d) {
        float near = 0, far = Float.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            float p = o.get(axis), v = d.get(axis);
            if (Math.abs(v) < 1e-7) {
                if (p < -1 || p > 1) return Float.POSITIVE_INFINITY;
            } else {
                float a = (-1 - p) / v, b = (1 - p) / v;
                near = Math.max(near, Math.min(a, b));
                far = Math.min(far, Math.max(a, b));
                if (far < near) return Float.POSITIVE_INFINITY;
            }
        }
        return near > 0 ? near : far;
    }

    private static float triangle(Vector3f o, Vector3f d, float[] v, int i) {
        Vector3f a = new Vector3f(v[i], v[i + 1], v[i + 2]);
        Vector3f e1 = new Vector3f(v[i + 8], v[i + 9], v[i + 10]).sub(a);
        Vector3f e2 = new Vector3f(v[i + 16], v[i + 17], v[i + 18]).sub(a);
        Vector3f p = new Vector3f(d).cross(e2);
        float det = e1.dot(p);
        if (Math.abs(det) < 1e-7) return Float.POSITIVE_INFINITY;
        Vector3f s = new Vector3f(o).sub(a);
        float u = s.dot(p) / det;
        if (u < 0 || u > 1) return Float.POSITIVE_INFINITY;
        Vector3f q = s.cross(e1);
        float w = d.dot(q) / det, t = e2.dot(q) / det;
        return w >= 0 && u + w <= 1 && t > 0 ? t : Float.POSITIVE_INFINITY;
    }
}
