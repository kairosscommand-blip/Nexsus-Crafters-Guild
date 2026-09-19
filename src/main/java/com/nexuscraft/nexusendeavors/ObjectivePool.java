package com.nexuscraft.nexusendeavors;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the daily and weekly objective pools from config.yml -- same defensive per-entry parsing
 * every loot/objective table in this project family uses: one malformed entry is skipped with a
 * logged warning, never fatal to the rest of the pool. {@link DailyBoard} picks a rotating subset
 * of each pool to actually be "today's"/"this week's" active objectives; everything not picked just
 * sits there as a candidate for another day.
 */
final class ObjectivePool {

    private final JavaPlugin plugin;
    private final List<ObjectiveDefinition> dailyPool = new ArrayList<>();
    private final List<ObjectiveDefinition> weeklyPool = new ArrayList<>();

    ObjectivePool(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        dailyPool.clear();
        weeklyPool.clear();
        for (Map<?, ?> raw : plugin.getConfig().getMapList("daily-pool")) {
            ObjectiveDefinition def = parse("daily-pool", raw);
            if (def != null) {
                dailyPool.add(def);
            }
        }
        for (Map<?, ?> raw : plugin.getConfig().getMapList("weekly-pool")) {
            ObjectiveDefinition def = parse("weekly-pool", raw);
            if (def != null) {
                weeklyPool.add(def);
            }
        }
    }

    private ObjectiveDefinition parse(String poolName, Map<?, ?> raw) {
        Object idObj = raw.get("id");
        Object keyObj = raw.get("objective-key");
        if (idObj == null || keyObj == null) {
            plugin.getLogger().warning("[NexusEndeavors] Skipping a malformed " + poolName
                    + " entry (missing id/objective-key): " + raw);
            return null;
        }
        int target = intOf(raw, "target", 0);
        if (target <= 0) {
            plugin.getLogger().warning("[NexusEndeavors] Skipping a malformed " + poolName
                    + " entry '" + idObj + "' (bad target): " + raw);
            return null;
        }
        String description = raw.get("description") != null ? String.valueOf(raw.get("description"))
                : String.valueOf(keyObj) + " x" + target;
        long rewardSeals = longOf(raw, "reward-seals", 0, poolName, idObj);
        return new ObjectiveDefinition(String.valueOf(idObj), String.valueOf(keyObj), description, target, rewardSeals);
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

    // reward-seals is a long (unlike target, which is a plain int) -- parsed separately so a
    // malformed or overflowing value is warned about rather than silently defaulting to 0, the
    // same honesty target's own "bad target" warning already gets.
    private long longOf(Map<?, ?> raw, String key, long def, String poolName, Object idObj) {
        Object v = raw.get(key);
        if (v == null) {
            return def;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("[NexusEndeavors] " + poolName + " entry '" + idObj
                    + "' has a malformed " + key + " value (" + v + ") -- defaulting to " + def + ".");
            return def;
        }
    }

    List<ObjectiveDefinition> dailyPool() {
        return dailyPool;
    }

    List<ObjectiveDefinition> weeklyPool() {
        return weeklyPool;
    }

    ObjectiveDefinition dailyById(String id) {
        for (ObjectiveDefinition def : dailyPool) {
            if (def.id().equalsIgnoreCase(id)) {
                return def;
            }
        }
        return null;
    }

    ObjectiveDefinition weeklyById(String id) {
        for (ObjectiveDefinition def : weeklyPool) {
            if (def.id().equalsIgnoreCase(id)) {
                return def;
            }
        }
        return null;
    }
}
