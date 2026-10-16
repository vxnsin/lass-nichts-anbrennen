package io.github.vxnsin.lna.plugin.map;

import java.util.List;
import java.util.Map;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

/** A map loaded from maps/*.yml: an ASCII floor plan, its legend and the mission data that belongs to it. */
public record MapDefinition(
        String id,
        String name,
        int height,
        BlockData floor,
        BlockData ceiling,
        BlockData light,
        int lightSpacing,
        BlockData frame,
        List<String> plan,
        Map<Character, Cell> legend,
        Map<String, String> texts,
        Map<String, Hazard> hazards,
        Map<String, String> safe) {

    public int width() {
        return plan.getFirst().length();
    }

    public int depth() {
        return plan.size();
    }

    /** Legend entry for plan column x (west to east) and row z (north to south). */
    public Cell cell(int x, int z) {
        return legend.get(plan.get(z).charAt(x));
    }

    /**
     * One legend entry. Fields that are not used by a character are null or empty.
     *
     * @param hazard / safe on a block cell: the block itself is clickable for missions
     * @param mission mission card lying on the block of this cell (mission table)
     * @param npc     player name whose skin a standing figure shows (e.g. the project author)
     */
    public record Cell(
            List<BlockData> wall,
            BlockData floor,
            boolean roof,
            BlockData door,
            BlockFace doorFacing,
            BlockData block,
            List<BlockData> pillar,
            BlockFace spawn,
            String text,
            String hazard,
            String safe,
            String mission,
            String npc,
            List<MapObject> objects) {

        public boolean isWall() {
            return !wall.isEmpty();
        }
    }

    /**
     * A custom item placed by the map.
     *
     * @param y         height above the floor (1 = standing on the floor)
     * @param collision overrides the item's collision setting, or null
     */
    public record MapObject(String id, BlockFace facing, int y, boolean prop, Boolean collision, String hazard,
                            String safe) {}

    /** A fire hazard players have to find in the hazard hunt. */
    public record Hazard(String title, String explanation) {}
}
