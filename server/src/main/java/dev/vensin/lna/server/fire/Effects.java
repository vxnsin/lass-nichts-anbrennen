package dev.vensin.lna.server.fire;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;

public final class Effects {

    private Effects() {}

    /** With a count of 0 the client reads the offsets as a velocity and {@code speed} as its length. */
    public static void particles(Instance instance, Particle particle, Point at, double offsetX, double offsetY,
                                 double offsetZ, double speed, int count) {
        instance.sendGroupedPacket(new ParticlePacket(particle, at.x(), at.y(), at.z(),
                (float) offsetX, (float) offsetY, (float) offsetZ, (float) speed, count));
    }

    public static void sound(Instance instance, SoundEvent sound, Point at, float volume, float pitch) {
        instance.playSound(Sound.sound(sound, Sound.Source.BLOCK, volume, pitch), at.x(), at.y(), at.z());
    }
}
