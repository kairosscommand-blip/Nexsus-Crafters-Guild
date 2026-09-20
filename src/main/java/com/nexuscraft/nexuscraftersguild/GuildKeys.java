package com.nexuscraft.nexuscraftersguild;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PersistentDataContainer keys used across this plugin -- same unspoofable-tag philosophy every
 * other Nexus plugin uses. Two different kinds of thing get tagged, on purpose never by name or
 * lore: a crafted item is tagged the instant CrafterCraftListener upgrades it (which forge made
 * it, which smith owned that forge, and the quality tier it rolled), and a world-event mob is
 * tagged the instant GuildEventScheduler spawns it (which event it belongs to) so
 * GuildEventListener can tell a Raider Incursion's own pack apart from every other hostile mob on
 * the server, including another Raider Incursion happening at the same time elsewhere.
 */
final class GuildKeys {

    final NamespacedKey forgedByForgeId;
    final NamespacedKey forgedBySmithId;
    final NamespacedKey forgedBySmithName;
    final NamespacedKey qualityTier;
    final NamespacedKey eventMobId;

    GuildKeys(JavaPlugin plugin) {
        this.forgedByForgeId = new NamespacedKey(plugin, "forged-by-forge-id");
        this.forgedBySmithId = new NamespacedKey(plugin, "forged-by-smith-id");
        this.forgedBySmithName = new NamespacedKey(plugin, "forged-by-smith-name");
        this.qualityTier = new NamespacedKey(plugin, "quality-tier");
        this.eventMobId = new NamespacedKey(plugin, "guild-event-id");
    }

    static final PersistentDataType<String, String> STRING = PersistentDataType.STRING;
}
