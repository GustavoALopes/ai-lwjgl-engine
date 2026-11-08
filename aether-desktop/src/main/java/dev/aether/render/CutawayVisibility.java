package dev.aether.render;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.scene.Transform;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** CPU-only occlusion tests; neither camera state nor visibility enters simulation. */
public final class CutawayVisibility {
    private CutawayVisibility() { }

    static Matrix4f model(Transform t) {
        var p = t.position(); var r = t.rotationDegrees(); var s = t.scale();
        return new Matrix4f().translate((float) p.x(), (float) p.y(), (float) p.z())
                .rotateXYZ((float) Math.toRadians(r.x()), (float) Math.toRadians(r.y()), (float) Math.toRadians(r.z()))
                .scale((float) s.x(), (float) s.y(), (float) s.z());
    }

    public static Set<Entity> obstructing(World world, Matrix4f projection, Matrix4f view) {
        Objects.requireNonNull(world, "world");
        Matrix4f vp = new Matrix4f(projection).mul(view);
        var camera = new Matrix4f(view).invert().getTranslation(new org.joml.Vector3f());
        Set<Entity> result = new LinkedHashSet<>();
        for (var surface : world.view(CutawaySurface.class).keySet()) {
            Transform wall = world.get(surface, Transform.class).orElse(null);
            if (wall == null || world.get(surface, MeshRenderer.class).isEmpty()) continue;
            var normal = world.get(surface, CutawaySurface.class).orElseThrow().outwardNormal();
            if (!normal.equals(dev.aether.math.Vec3.ZERO)) {
                // Room cutaways depend on which side the camera is on, so
                // foreground walls stay lowered as the player moves about.
                double facing = (camera.x - wall.position().x()) * normal.x()
                        + (camera.y - wall.position().y()) * normal.y()
                        + (camera.z - wall.position().z()) * normal.z();
                if (facing > 1e-4) result.add(surface);
                continue;
            }
            Matrix4f wallModel = model(wall);
            if (Math.abs(wallModel.determinant()) < 1e-12) continue;
            Matrix4f wallClip = new Matrix4f(vp).mul(wallModel);
            for (var target : world.view(CutawayTarget.class).entrySet()) {
                Transform body = world.get(target.getKey(), Transform.class).orElse(null);
                if (surface.equals(target.getKey()) || body == null) continue;
                // Build a conservative protected screen rectangle from all box
                // corners. Unlike sparse rays, this cannot miss a thin wall
                // between samples. It may fade a little extra near silhouettes.
                Matrix4f targetModel = model(new Transform(body.position(), body.rotationDegrees(),
                        new dev.aether.math.Vec3(Math.abs(body.scale().x()) + target.getValue().padding(),
                                Math.abs(body.scale().y()) + target.getValue().padding(),
                                Math.abs(body.scale().z()) + target.getValue().padding())));
                Matrix4f targetClip = new Matrix4f(vp).mul(targetModel);
                float left = Float.POSITIVE_INFINITY, right = Float.NEGATIVE_INFINITY;
                float bottom = Float.POSITIVE_INFINITY, top = Float.NEGATIVE_INFINITY, far = Float.NEGATIVE_INFINITY;
                for (int x = -1; x <= 1; x += 2)
                    for (int y = -1; y <= 1; y += 2)
                        for (int z = -1; z <= 1; z += 2) {
                            Vector4f clip = targetClip.transform(new Vector4f(x, y, z, 1));
                            if (clip.w <= 0) continue;
                            left = Math.min(left, clip.x / clip.w);
                            right = Math.max(right, clip.x / clip.w);
                            bottom = Math.min(bottom, clip.y / clip.w);
                            top = Math.max(top, clip.y / clip.w);
                            far = Math.max(far, clip.z / clip.w);
                        }
                left = Math.max(-1, left); right = Math.min(1, right);
                bottom = Math.max(-1, bottom); top = Math.min(1, top);
                if (left > right || bottom > top || far < -1) continue;
                if (intersectsProtectedFrustum(wallClip, left, right, bottom, top, Math.min(1, far))) {
                    result.add(surface);
                    break;
                }
            }
        }
        return java.util.Collections.unmodifiableSet(result);
    }

    private static boolean intersectsProtectedFrustum(Matrix4f wallClip, float left, float right,
                                                      float bottom, float top, float far) {
        boolean[] outside = {true, true, true, true, true, true};
        for (int x = -1; x <= 1; x += 2)
            for (int y = -1; y <= 1; y += 2)
                for (int z = -1; z <= 1; z += 2) {
                    Vector4f p = wallClip.transform(new Vector4f(x, y, z, 1));
                    outside[0] &= p.x < left * p.w;
                    outside[1] &= p.x > right * p.w;
                    outside[2] &= p.y < bottom * p.w;
                    outside[3] &= p.y > top * p.w;
                    outside[4] &= p.z < -p.w;
                    outside[5] &= p.z > far * p.w;
                }
        for (boolean allOutside : outside) if (allOutside) return false;
        return true;
    }

    static float approach(float current, float target, double elapsedSeconds) {
        float step = (float) (Math.max(0, Math.min(.1, elapsedSeconds)) / .2);
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }
}
