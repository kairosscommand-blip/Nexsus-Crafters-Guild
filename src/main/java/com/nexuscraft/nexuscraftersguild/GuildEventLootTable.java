package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Weighted loot pools for world-event chests (Rich Vein's find, a cleared Supply Caravan's goods),
 * loaded from {@code events.loot.<pool>} in config.yml -- same shape and same defensive per-entry
 * parsing as NexusDungeons' own DungeonLootTable, copied and renamed for this plugin: one malformed
 * config entry is skipped with a logged warning, never fatal to the rest of the pool.
 */
final class GuildEventLootTable {

    private final JavaPlugin plugin;
    private final Map<String, List<LootItem>> pools = new HashMap<>();

    GuildEventLootTable(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        pools.clear();
        ConfigurationSection loot = plugin.getConfig().getConfigurationSection("events.loot");
        if (loot == null) {
            return;
        }
        for (String poolName : loot.getKeys(false)) {
            ConfigurationSection poolSection = loot.getConfigurationSection(poolName);
            if (poolSection == null) {
                continue;
            }
            List<LootItem> items = new ArrayList<>();
            for (Map<?, ?> raw : poolSection.getMapList("items")) {
                LootItem item = parseItem(poolName, raw);
                if (item != null) {
                    items.add(item);
                }
            }
            pools.put(poolName.toLowerCase(Locale.ROOT), items);
        }
    }

    private LootItem parseItem(String poolName, Map<?, ?> raw) {
        Object materialObj = raw.get("material");
        Material material = materialObj != null ? Material.matchMaterial(String.valueOf(materialObj)) : null;
        if (material == null) {
            plugin.getLogger().warning("[NexusCraftersGuild] Skipping a malformed loot entry in pool '"
                    + poolName + "' (bad/missing material): " + raw);
            return null;
        }
        int min = intOf(raw, "min", 1);
        int max = intOf(raw, "max", Math.max(1, min));
        int weight = intOf(raw, "weight", 0);
        if (weight <= 0 || min <= 0 || max < min) {
            plugin.getLogger().warning("[NexusCraftersGuild] Skipping a malformed loot entry in pool '"
                    + poolName + "' (bad min/max/weight): " + raw);
            return null;
        }
        return new LootItem(material, min, max, weight);
    }

    private int intOf(Map<?, ?> raw, String key, int def) {
        Object v = raw.get(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    List<ItemStack> roll(String pool, int stackCountMin, int stackCountMax, Random random) {
        List<ItemStack> result = new ArrayList<>();
        List<LootItem> items = pools.get(pool.toLowerCase(Locale.ROOT));
        if (items == null || items.isEmpty()) {
            return result;
        }
        int totalWeight = 0;
        for (LootItem item : items) {
            totalWeight += item.weight();
        }
        if (totalWeight <= 0) {
            return result;
        }
        int stackCount = stackCountMin + (stackCountMax > stackCountMin
                ? random.nextInt(stackCountMax - stackCountMin + 1) : 0);
        for (int i = 0; i < stackCount; i++) {
            int roll = random.nextInt(totalWeight);
            int cursor = 0;
            for (LootItem item : items) {
                cursor += item.weight();
                if (roll < cursor) {
                    int amount = item.min() + (item.max() > item.min()
                            ? random.nextInt(item.max() - item.min() + 1) : 0);
                    result.add(new ItemStack(item.material(), amount));
                    break;
                }
            }
        }
        return result;
    }
}
