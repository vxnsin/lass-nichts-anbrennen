package dev.vensin.lna.server.lobby;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public final class Rays {

    private Rays() {}

    /** Distance along the ray to the box, or infinity if it misses (slab method). */
    public static double distanceToBox(Point origin, Vec direction, double minX, double minY, double minZ,
                                       double maxX, double maxY, double maxZ) {
        double near = 0;
        double far = Double.POSITIVE_INFINITY;
        double[] start = {origin.x(), origin.y(), origin.z()};
        double[] step = {direction.x(), direction.y(), direction.z()};
        double[] min = {minX, minY, minZ};
        double[] max = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(step[axis]) < 1e-9) {
                if (start[axis] < min[axis] || start[axis] > max[axis]) {
                    return Double.POSITIVE_INFINITY;
                }
                continue;
            }
            double a = (min[axis] - start[axis]) / step[axis];
            double b = (max[axis] - start[axis]) / step[axis];
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
        }
        return near <= far ? near : Double.POSITIVE_INFINITY;
    }
}
