package dev.vensin.lna.server.map;

import java.util.List;
import java.util.Map;

import net.minestom.server.instance.block.Block;

public record MapDefinition(
        String id,
        String name,
        int height,
        Block floor,
        Block ceiling,
        Block light,
        int lightSpacing,
        Block frame,
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

    public record Cell(
            List<Block> wall,
            Block floor,
            boolean roof,
            Block door,
            Facing doorFacing,
            Block block,
            List<Block> pillar,
            Facing spawn,
            String text,
            String hazard,
            String safe,
            String mission,
            String npc,
            FireSpot fire,
            String action,
            List<MapObject> objects) {

        public boolean isWall() {
            return !wall.isEmpty();
        }
    }

    /** {@code y} is the height above the floor (1 = standing on the floor); a non-null {@code collision} overrides the item. */
    public record MapObject(String id, Facing facing, int y, boolean prop, Boolean collision, String hazard,
                            String safe, boolean give) {}

    /** {@code y}: 2 = on top of a block or furniture. {@code solution}: null (any suitable extinguisher), "lid" or "valve". */
    public record FireSpot(String fireClass, int order, int y, String title, String hint, String solution) {}

    public record Hazard(String title, String explanation) {}
}
