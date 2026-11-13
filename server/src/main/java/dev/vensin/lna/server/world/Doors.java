package dev.vensin.lna.server.world;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;

/** Minestom has no vanilla block behaviour, so opening doors is handled here. Iron doors stay shut. */
public final class Doors {

    private Doors() {}

    public static void onInteract(PlayerBlockInteractEvent event) {
        Block block = event.getBlock();
        String name = block.key().value();
        if (!name.endsWith("_door") || name.equals("iron_door")) {
            return;
        }
        boolean open = !"true".equals(block.getProperty("open"));
        Instance instance = event.getInstance();
        int x = event.getBlockPosition().blockX();
        int y = event.getBlockPosition().blockY();
        int z = event.getBlockPosition().blockZ();
        int otherY = "upper".equals(block.getProperty("half")) ? y - 1 : y + 1;
        instance.setBlock(x, y, z, block.withProperty("open", String.valueOf(open)));
        Block other = instance.getBlock(x, otherY, z);
        if (other.key().equals(block.key())) {
            instance.setBlock(x, otherY, z, other.withProperty("open", String.valueOf(open)));
        }
        instance.playSound(Sound.sound(open ? SoundEvent.BLOCK_WOODEN_DOOR_OPEN : SoundEvent.BLOCK_WOODEN_DOOR_CLOSE,
                Sound.Source.BLOCK, 1f, 1f), x + 0.5, y + 0.5, z + 0.5);
        event.setBlockingItemUse(true);
    }
}
