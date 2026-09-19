package com.nexuscraft.nexusendeavors;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists every player's endeavor standing to endeavors.yml -- same flat list-of-maps pattern as
 * every registry in this family. Per-objective progress is encoded as {@code "id:amount"} strings
 * (same "map-like data as encoded string lines, not nested YAML paths" convention HouseRegistry's
 * own member list uses) rather than nested under each objective's own id, since a future config
 * change could in principle give an objective an id that collides with a YAML-reserved character.
 *
 * <p>Deliberately NOT saved on every single reportProgress() call -- a busy server reporting kills/
 * crafts/mining constantly would mean real, needless I/O pressure. Progress lives in memory during
 * play and is flushed periodically plus once for certain on disable, same tradeoff SmithRegistry's
 * own comment documents for NexusCraftersGuild.
 */
final class EndeavorRegistry {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerEndeavorProgress> profiles = new LinkedHashMap<>();

    EndeavorRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "endeavors.yml");
    }

    void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        profiles.clear();
        for (Map<?, ?> raw : data.getMapList("players")) {
            try {
                PlayerEndeavorProgress profile = fromMap(raw);
                profiles.put(profile.playerId, profile);
            } catch (Exception e) {
                plugin.getLogger().warning("[NexusEndeavors] Skipping a corrupt endeavor entry: " + e.getMessage());
            }
        }
    }

    void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PlayerEndeavorProgress profile : profiles.values()) {
            list.add(toMap(profile));
        }
        YamlConfiguration data = new YamlConfiguration();
        data.set("players", list);
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[NexusEndeavors] Could not save endeavors.yml: " + e.getMessage());
        }
    }

    PlayerEndeavorProgress getOrCreate(UUID playerId, String playerName) {
        PlayerEndeavorProgress profile = profiles.get(playerId);
        if (profile == null) {
            profile = new PlayerEndeavorProgress(playerId, playerName);
            profiles.put(playerId, profile);
        } else if (playerName != null) {
            profile.playerName = playerName;
        }
        return profile;
    }

    PlayerEndeavorProgress find(UUID playerId) {
        return profiles.get(playerId);
    }

    private static Map<String, Object> toMap(PlayerEndeavorProgress p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("player-id", p.playerId.toString());
        m.put("player-name", p.playerName);
        m.put("seals", p.seals);
        m.put("lifetime-seals-earned", p.lifetimeSealsEarned);
        m.put("login-streak", p.loginStreak);
        m.put("last-login-day-index", p.lastLoginDayIndex);
        m.put("daily-reset-day-index", p.dailyResetDayIndex);
        m.put("weekly-reset-week-index", p.weeklyResetWeekIndex);
        m.put("daily-progress", encode(p.dailyProgress));
        m.put("daily-completed", new ArrayList<>(p.dailyCompleted));
        m.put("weekly-progress", encode(p.weeklyProgress));
        m.put("weekly-completed", new ArrayList<>(p.weeklyCompleted));
        return m;
    }

    private static List<String> encode(Map<String, Integer> progress) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : progress.entrySet()) {
            lines.add(entry.getKey() + ":" + entry.getValue());
        }
        return lines;
    }

    private static PlayerEndeavorProgress fromMap(Map<?, ?> m) {
        UUID id = UUID.fromString(String.valueOf(m.get("player-id")));
        String name = m.get("player-name") != null ? String.valueOf(m.get("player-name")) : "?";
        PlayerEndeavorProgress p = new PlayerEndeavorProgress(id, name);
        p.seals = longOf(m, "seals");
        p.lifetimeSealsEarned = longOf(m, "lifetime-seals-earned");
        p.loginStreak = (int) longOf(m, "login-streak");
        p.lastLoginDayIndex = longOfOrMin(m, "last-login-day-index");
        p.dailyResetDayIndex = longOfOrMin(m, "daily-reset-day-index");
        p.weeklyResetWeekIndex = longOfOrMin(m, "weekly-reset-week-index");
        decode(m.get("daily-progress"), p.dailyProgress);
        decodeList(m.get("daily-completed"), p.dailyCompleted);
        decode(m.get("weekly-progress"), p.weeklyProgress);
        decodeList(m.get("weekly-completed"), p.weeklyCompleted);
        return p;
    }

    private static void decode(Object raw, Map<String, Integer> target) {
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object entry : list) {
            String line = String.valueOf(entry);
            int split = line.lastIndexOf(':');
            if (split <= 0) {
                continue;
            }
            try {
                target.put(line.substring(0, split), Integer.parseInt(line.substring(split + 1)));
            } catch (NumberFormatException ignored) {
                // one corrupt progress line shouldn't drop the rest of this player's board
            }
        }
    }

    private static void decodeList(Object raw, java.util.Set<String> target) {
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object entry : list) {
            target.add(String.valueOf(entry));
        }
    }

    private static long longOf(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? 0L : Long.parseLong(String.valueOf(v));
    }

    private static long longOfOrMin(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? Long.MIN_VALUE : Long.parseLong(String.valueOf(v));
    }
}
