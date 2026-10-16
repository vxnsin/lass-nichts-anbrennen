package io.github.vxnsin.lna.plugin.fire;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.block.Block;

/** One simulated fire: an origin cell, a fire class and an intensity from 0 to 100 that controls its size. */
public final class Fire {

    public enum State {
        BURNING,
        /** Flames are out but embers remain; without further spraying the fire reignites (class A). */
        SMOLDERING
    }

    static final double MAX_INTENSITY = 100;
    private static final double MAX_RADIUS = 2.5;

    private final UUID id = UUID.randomUUID();
    private final Block origin;
    private final FireClass fireClass;
    private double intensity;
    private State state = State.BURNING;
    private int smolderCycles;
    private long lastFlare;
    private Set<Block> flames = new HashSet<>();

    Fire(Block origin, FireClass fireClass, double intensity) {
        this.origin = origin;
        this.fireClass = fireClass;
        this.intensity = intensity;
    }

    public UUID id() {
        return id;
    }

    /** The cell the fire starts from (an air block above the burning object). */
    public Block origin() {
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

    /** Spread radius in blocks around the origin. */
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

    Set<Block> flames() {
        return flames;
    }

    void flames(Set<Block> value) {
        flames = value;
    }
}
