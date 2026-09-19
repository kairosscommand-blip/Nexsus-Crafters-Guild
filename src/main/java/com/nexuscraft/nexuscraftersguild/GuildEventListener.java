package com.nexuscraft.nexuscraftersguild;

import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.UUID;

/**
 * Reacts to what players actually do against a live world event: credits anyone who lands a hit on
 * a tagged event mob (not just whoever gets the killing blow -- see EntityDamageByEntityEvent
 * below), notices when an event's last tagged mob falls, and notices when someone opens a world
 * event's chest for the first time.
 */
final class GuildEventListener implements Listener {

    private final GuildEventRegistry registry;
    private final GuildEventScheduler scheduler;
    private final GuildKeys keys;

    GuildEventListener(GuildEventRegistry registry, GuildEventScheduler scheduler, GuildKeys keys) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.keys = keys;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity damaged)) {
            return;
        }
        WorldEvent worldEvent = eventOf(damaged);
        if (worldEvent == null || worldEvent.status != WorldEvent.Status.ACTIVE) {
            return;
        }
        worldEvent.participantIds.add(player.getUniqueId());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        WorldEvent worldEvent = eventOf(entity);
        if (worldEvent == null || worldEvent.status != WorldEvent.Status.ACTIVE) {
            return;
        }
        worldEvent.mobsRemaining = Math.max(0, worldEvent.mobsRemaining - 1);
        if (worldEvent.mobsRemaining > 0) {
            return;
        }
        worldEvent.status = WorldEvent.Status.COMPLETE;
        switch (worldEvent.type) {
            case RAIDER_INCURSION -> scheduler.completeRaiderIncursion(worldEvent);
            case SUPPLY_CARAVAN -> scheduler.completeCaravanClear(worldEvent);
            case RICH_VEIN -> {
                // RICH_VEIN never spawns mobs -- unreachable, kept only so this switch stays
                // exhaustive against every EventType without a default case masking a future one.
            }
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof Chest chest)) {
            return;
        }
        var location = chest.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        for (WorldEvent worldEvent : registry.all()) {
            if (worldEvent.chestLooted || worldEvent.chestX == null) {
                continue;
            }
            if (!worldEvent.world.equals(location.getWorld().getName())
                    || worldEvent.chestX != location.getBlockX()
                    || worldEvent.chestY != location.getBlockY()
                    || worldEvent.chestZ != location.getBlockZ()) {
                continue;
            }
            worldEvent.chestLooted = true;
            if (worldEvent.type == EventType.RICH_VEIN) {
                scheduler.awardRichVeinFinder(player);
            } else if (worldEvent.type == EventType.SUPPLY_CARAVAN) {
                scheduler.awardCaravanFinder(player);
            }
            return;
        }
    }

    private WorldEvent eventOf(LivingEntity entity) {
        String raw = entity.getPersistentDataContainer().get(keys.eventMobId, GuildKeys.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return registry.byId(UUID.fromString(raw));
        } catch (IllegalArgumentException badId) {
            return null;
        }
    }
}
