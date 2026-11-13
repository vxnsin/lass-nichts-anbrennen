package dev.vensin.lna.server.map;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;

/** x, y, z is the north-west floor corner. */
public record PlacedMap(String id, int x, int y, int z, int width, int depth, int height, Pos spawn) {

    public boolean contains(Point point) {
        return point.x() >= x && point.x() < x + width
                && point.z() >= z && point.z() < z + depth
                && point.y() >= y && point.y() < y + height + 2;
    }
}
