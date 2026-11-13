package dev.vensin.lna.server;

import net.minestom.server.tag.Tag;

public final class Tags {

    public static final Tag<String> ITEM_ID = Tag.String("lna_id");
    public static final Tag<String> FURNITURE_ID = Tag.String("lna_furniture");
    /** Number of barrier blocks above the hitbox base. */
    public static final Tag<Integer> FURNITURE_CELLS = Tag.Integer("lna_cells");
    public static final Tag<String> MAP = Tag.String("lna_map");
    public static final Tag<String> HAZARD = Tag.String("lna_hazard");
    public static final Tag<String> SAFE = Tag.String("lna_safe");
    public static final Tag<String> GIVE = Tag.String("lna_give");
    public static final Tag<String> ACTION = Tag.String("lna_action");
    public static final Tag<String> MISSION = Tag.String("lna_mission");
    public static final Tag<String> NPC = Tag.String("lna_npc");
    public static final Tag<Integer> FILL = Tag.Integer("lna_fill");

    private Tags() {}
}
