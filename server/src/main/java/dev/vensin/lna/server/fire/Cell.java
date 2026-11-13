package dev.vensin.lna.server.fire;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

/** One block position in an instance. */
public record Cell(Instance instance, int x, int y, int z) {

    public Cell offset(int dx, int dy, int dz) {
        return new Cell(instance, x + dx, y + dy, z + dz);
    }

    public Pos center(double height) {
        return new Pos(x + 0.5, y + height, z + 0.5);
    }
}
