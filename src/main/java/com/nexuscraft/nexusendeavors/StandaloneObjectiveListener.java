package com.nexuscraft.nexusendeavors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Objective keys that work with zero dependency on any other Nexus plugin -- pure vanilla Bukkit
 * events. These are what let NexusEndeavors ship a real, immediately-working daily board even on a
 * server that has none of NexusDungeons/NexusCraftersGuild/NexusHouses installed at all; the three
 * plugin-specific keys (see each of their own new EndeavorsBridge classes) are additive on top of
 * this, not a replacement for it.
 */
final class StandaloneObjectiveListener implements Listener {

    private static final Set<EntityType> HOSTILE_TYPES = EnumSet.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON,
            EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.ENDERMAN,
            EntityType.ENDERMITE, EntityType.DROWNED, EntityType.VINDICATOR,
            EntityType.SILVERFISH, EntityType.WITCH, EntityType.EVOKER, EntityType.VEX);

    private final EndeavorProgressEngine engine;
    private final Set<Material> oreMaterials;
    // Per-player accumulated move distance since the last whole-block report -- reporting every
    // single PlayerMoveEvent call as "1 block" would wildly over-count sub-block camera-look
    // moves; this only reports once real accumulated distance crosses a full block.
    private final Map<UUID, Double> walkAccumulator = new HashMap<>();

    StandaloneObjectiveListener(JavaPlugin plugin, EndeavorProgressEngine engine) {
        this.engine = engine;
        this.oreMaterials = EnumSet.noneOf(Material.class);
        for (String name : plugin.getConfig().getStringList("standalone.ore-materials")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                oreMaterials.add(material);
            } else {
                plugin.getLogger().warning("[NexusEndeavors] Unknown material in standalone.ore-materials: " + name);
            }
        }
        if (oreMaterials.isEmpty()) {
            oreMaterials.add(Material.IRON_ORE);
            oreMaterials.add(Material.DIAMOND_ORE);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !HOSTILE_TYPES.contains(entity.getType())) {
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        engine.reportProgress(killer.getUniqueId(), killer.getName(), "kill.hostile", 1);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer() == null || event.getBlock() == null) {
            return;
        }
        if (!oreMaterials.contains(event.getBlock().getType())) {
            return;
        }
        Player player = event.getPlayer();
        engine.reportProgress(player.getUniqueId(), player.getName(), "mine.ore", 1);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().getName().equals(to.getWorld().getName())) {
            return;
        }
        double distance = Math.sqrt(from.distanceSquared(to));
        if (distance <= 0 || distance > 10) {
            return; // a >10-block single "move" is a teleport, not walking -- don't count it
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        double accumulated = walkAccumulator.merge(id, distance, Double::sum);
        if (accumulated < 1.0) {
            return;
        }
        int wholeBlocks = (int) Math.floor(accumulated);
        walkAccumulator.put(id, accumulated - wholeBlocks);
        engine.reportProgress(id, player.getName(), "walk.blocks", wholeBlocks);
    }
}
