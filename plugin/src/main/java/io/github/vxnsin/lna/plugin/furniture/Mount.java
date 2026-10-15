package io.github.vxnsin.lna.plugin.furniture;

/** Where a placed object attaches. Models follow the conventions of tools/assetgen/model.py. */
public enum Mount {
    /** Stands on the floor; the model front (north face) turns towards the placing player. */
    FLOOR,
    /** Hangs on the clicked wall; the model back (z = 16) touches the wall. */
    WALL,
    /** Hangs from the clicked ceiling. */
    CEILING
}
