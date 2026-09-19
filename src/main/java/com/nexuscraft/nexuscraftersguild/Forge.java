package com.nexuscraft.nexuscraftersguild;

import java.util.UUID;

/**
 * A claimed patch of ground -- "a placeable forge business," per the ask. Nothing about a forge is
 * tied to a specific block: any real vanilla Crafter block a player later places anywhere inside
 * the claimed radius is automatically "this forge's," for as long as it stays inside the circle
 * (see ForgeRegistry#forgeAt) -- claim the ground once, build and rebuild your actual smithy layout
 * freely afterward, add more Crafters to it, move a hopper chain around, without ever re-claiming.
 */
final class Forge {

    final UUID id;
    final String name;
    final UUID ownerId;
    String ownerName;
    final String world;
    final int x;
    final int y;
    final int z;
    final int radius;
    long itemsForged;

    /** Throttle bookkeeping -- deliberately NOT persisted (see ForgeRegistry's own comment): how
     *  much XP this forge has already fed its owner in the current one-minute window, and when that
     *  window started. Keeps a single unattended hopper clock from instantly maxing a smith out --
     *  see CrafterCraftListener. */
    transient long xpThisWindow;
    transient long xpWindowStartMillis;

    Forge(UUID id, String name, UUID ownerId, String ownerName, String world, int x, int y, int z, int radius) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    boolean contains(String worldName, double bx, double by, double bz) {
        if (!this.world.equals(worldName)) {
            return false;
        }
        double dx = bx - x;
        double dz = bz - z;
        return dx * dx + dz * dz <= (double) radius * radius;
    }
}
