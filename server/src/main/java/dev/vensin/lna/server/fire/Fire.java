package dev.vensin.lna.server.fire;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Fire {

    public enum State {
        BURNING,
        SMOLDERING
    }

    static final double MAX_INTENSITY = 100;
    private static final double MAX_RADIUS = 2.5;

    private final UUID id = UUID.randomUUID();
    private final Cell origin;
    private final FireClass fireClass;
    private double intensity;
    private State state = State.BURNING;
    private int smolderCycles;
    private long lastFlare;
    private Set<Cell> flames = new HashSet<>();
    private final boolean fed;

    Fire(Cell origin, FireClass fireClass, double intensity, boolean fed) {
        this.origin = origin;
        this.fireClass = fireClass;
        this.intensity = intensity;
        this.fed = fed;
    }

    /** Fuel or oxygen keeps flowing in (e.g. an open gas valve): spraying does not help, only a closed valve or a lid. */
    public boolean fed() {
        return fed;
    }

    public UUID id() {
        return id;
    }

    /** The cell the fire starts from (an air block above the burning object). */
    public Cell origin() {
        return origin;
    }

    public FireClass fireClass() {
        return fireClass;
    }

    public double intensity() {
        return intensity;
    }

    public State state() {
        return state;
    }

    double radius() {
        return Math.min(MAX_RADIUS, intensity / 40.0);
    }

    void intensity(double value) {
        intensity = Math.clamp(value, 0, MAX_INTENSITY);
    }

    void state(State value) {
        state = value;
    }

    int smolderCycles() {
        return smolderCycles;
    }

    void smolderCycles(int value) {
        smolderCycles = value;
    }

    long lastFlare() {
        return lastFlare;
    }

    void lastFlare(long value) {
        lastFlare = value;
    }

    Set<Cell> flames() {
        return flames;
    }

    void flames(Set<Cell> value) {
        flames = value;
    }
}
