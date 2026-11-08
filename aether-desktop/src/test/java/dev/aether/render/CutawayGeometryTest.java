package dev.aether.render;

import dev.aether.math.Vec3;
import dev.aether.scene.Transform;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CutawayGeometryTest {
    @Test void shortensWallToSolidLedgeWithoutMovingItsBaseOrChangingOriginal() {
        var original = new Transform(new Vec3(0, 1.8, 6.2), Vec3.ZERO, new Vec3(8.4, 3, .2));
        var drawn = CutawayGeometry.lowered(original, .6, 1);
        assertEquals(.6, drawn.scale().y() * 2, 1e-9);
        assertEquals(-1.2, drawn.position().y() - drawn.scale().y(), 1e-6);
        assertEquals(-.6, drawn.position().y() + drawn.scale().y(), 1e-6);
        assertEquals(3, original.scale().y());
        assertEquals(1.8, original.position().y());
        assertEquals(original.scale().x(), drawn.scale().x());
        assertEquals(original.scale().z(), drawn.scale().z());
        assertEquals(original, CutawayGeometry.lowered(original, .6, 0));
    }

    @Test void preservesBaseForRotatedAndNegativeScaleWalls() {
        for (double scale : new double[] {3, -3}) {
            var original = new Transform(new Vec3(3, 4, 5), new Vec3(15, 35, 10), new Vec3(8, scale, .2));
            var lowered = CutawayGeometry.lowered(original, .6, .5);
            var base = CutawayVisibility.model(original).transformPosition(new Vector3f(0, -1, 0));
            var drawnBase = CutawayVisibility.model(lowered).transformPosition(new Vector3f(0, -1, 0));
            assertEquals(base.x, drawnBase.x, 1e-5);
            assertEquals(base.y, drawnBase.y, 1e-5);
            assertEquals(base.z, drawnBase.z, 1e-5);
            assertEquals(3.3, Math.abs(lowered.scale().y()) * 2, 1e-9);
        }
    }

    @Test void neverGrowsShortWallsAndSupportsCompleteCut() {
        var wall = new Transform(Vec3.ZERO, Vec3.ZERO, new Vec3(2, .2, .2));
        assertEquals(wall, CutawayGeometry.lowered(wall, .6, 1));
        assertEquals(0, CutawayGeometry.lowered(wall, 0, 1).scale().y());
        assertThrows(IllegalArgumentException.class, () -> CutawayGeometry.lowered(wall, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> CutawayGeometry.lowered(wall, .6, Double.NaN));
    }
}
