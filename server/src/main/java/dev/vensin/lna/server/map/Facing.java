package dev.vensin.lna.server.map;

public enum Facing {
    NORTH(0, -1, 180f),
    EAST(1, 0, -90f),
    SOUTH(0, 1, 0f),
    WEST(-1, 0, 90f);

    private final int dx;
    private final int dz;
    private final float yaw;

    Facing(int dx, int dz, float yaw) {
        this.dx = dx;
        this.dz = dz;
        this.yaw = yaw;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    /** Minecraft yaw of a player looking this way. */
    public float yaw() {
        return yaw;
    }
}
