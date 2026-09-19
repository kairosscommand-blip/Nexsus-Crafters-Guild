package com.nexuscraft.nexuscraftersguild;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One live world event -- entirely in-memory, never written to disk (see GuildEventScheduler's
 * own comment: these are short-lived by design, a server restart mid-event just means it doesn't
 * pick back up, same honest simplification NexusDungeons' own generation-doesn't-resume limitation
 * documents for a very similar reason).
 */
final class WorldEvent {

    enum Status {
        ACTIVE,
        COMPLETE,
        EXPIRED
    }

    final UUID id;
    final EventType type;
    final String world;
    final int x;
    final int y;
    final int z;
    final long expiresAtMillis;

    Status status = Status.ACTIVE;
    int mobsRemaining;
    final Set<UUID> participantIds = new LinkedHashSet<>();

    /** RICH_VEIN: set at spawn. SUPPLY_CARAVAN: only set once its guards are cleared -- see
     *  GuildEventScheduler#revealCaravanChest. RAIDER_INCURSION never uses these (no chest). */
    Integer chestX;
    Integer chestY;
    Integer chestZ;
    boolean chestLooted;

    WorldEvent(UUID id, EventType type, String world, int x, int y, int z, long expiresAtMillis) {
        this.id = id;
        this.type = type;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.expiresAtMillis = expiresAtMillis;
    }

    boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
