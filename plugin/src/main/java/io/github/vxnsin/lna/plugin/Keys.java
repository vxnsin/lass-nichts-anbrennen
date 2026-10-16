package io.github.vxnsin.lna.plugin;

import org.bukkit.NamespacedKey;

/** Persistent data keys stored on item stacks. */
public final class Keys {

    /** Resource namespace, short for "Lass nichts anbrennen". */
    public static final String NAMESPACE = "lna";

    /** Custom item id, e.g. "feuerloescher_abc". Items are identified by this key, never by display name. */
    public static final NamespacedKey ITEM_ID = new NamespacedKey(NAMESPACE, "id");
    /** Remaining fill level of an extinguisher. */
    public static final NamespacedKey FILL = new NamespacedKey(NAMESPACE, "fill");

    /** Item id of a placed object, stored on its interaction hitbox. */
    public static final NamespacedKey FURNITURE_ID = new NamespacedKey(NAMESPACE, "furniture");
    /** UUID of the item display that renders a placed object. */
    public static final NamespacedKey FURNITURE_DISPLAY = new NamespacedKey(NAMESPACE, "display");
    /** Number of barrier blocks (collision) above the hitbox base. */
    public static final NamespacedKey FURNITURE_CELLS = new NamespacedKey(NAMESPACE, "cells");

    /** Id of the map that created an entity (removed when the map is rebuilt). */
    public static final NamespacedKey MAP = new NamespacedKey(NAMESPACE, "map");
    /** Hazard id of a clickable object (hazard hunt). */
    public static final NamespacedKey HAZARD = new NamespacedKey(NAMESPACE, "hazard");
    /** Id of a clickable object that is deliberately fine (hazard hunt false alarm). */
    public static final NamespacedKey SAFE = new NamespacedKey(NAMESPACE, "safe");
    /** Mission id of a clickable mission board entry. */
    public static final NamespacedKey MISSION = new NamespacedKey(NAMESPACE, "mission");

    private Keys() {}
}
