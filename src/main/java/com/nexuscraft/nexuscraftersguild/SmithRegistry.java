package com.nexuscraft.nexuscraftersguild;

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
 * Persists every player's guild standing to smiths.yml -- a flat list of maps, same
 * list-of-maps-not-path-segments pattern every registry in this project family uses since a player
 * name is free text a YAML path can't safely use as a key.
 *
 * <p>Deliberately NOT saved on every single XP gain: a busy automation chain can trigger dozens of
 * crafts a second, and flushing to disk that often would be real, needless I/O pressure on a
 * feature whose entire point is "leave it running unattended." Profiles live in memory during play
 * and are flushed periodically (NexusCraftersGuild's own scheduled task) and once for certain on
 * plugin disable -- an untimely server crash can lose at most that window's worth of XP, not a
 * whole profile.
 */
final class SmithRegistry {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, SmithProfile> profiles = new LinkedHashMap<>();

    SmithRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "smiths.yml");
    }

    void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        profiles.clear();
        for (Map<?, ?> raw : data.getMapList("smiths")) {
            try {
                SmithProfile profile = fromMap(raw);
                profiles.put(profile.playerId, profile);
            } catch (Exception e) {
                plugin.getLogger().warning("[NexusCraftersGuild] Skipping a corrupt smith entry: " + e.getMessage());
            }
        }
    }

    void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SmithProfile profile : profiles.values()) {
            list.add(toMap(profile));
        }
        YamlConfiguration data = new YamlConfiguration();
        data.set("smiths", list);
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[NexusCraftersGuild] Could not save smiths.yml: " + e.getMessage());
        }
    }

    SmithProfile getOrCreate(UUID playerId, String playerName) {
        SmithProfile profile = profiles.get(playerId);
        if (profile == null) {
            profile = new SmithProfile(playerId, playerName);
            profiles.put(playerId, profile);
        } else if (playerName != null) {
            profile.playerName = playerName;
        }
        return profile;
    }

    SmithProfile find(UUID playerId) {
        return profiles.get(playerId);
    }

    /** Returns the level BEFORE the gain, so callers can compare it against profile.level()
     *  afterward to tell whether the smith just leveled up. */
    int addXp(SmithProfile profile, long amount) {
        int before = profile.level();
        profile.xp += Math.max(0, amount);
        return before;
    }

    void addReputation(SmithProfile profile, int amount) {
        profile.reputation = Math.max(0, profile.reputation + amount);
    }

    private static Map<String, Object> toMap(SmithProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("player-id", p.playerId.toString());
        m.put("player-name", p.playerName);
        m.put("xp", p.xp);
        m.put("reputation", p.reputation);
        return m;
    }

    private static SmithProfile fromMap(Map<?, ?> m) {
        UUID id = UUID.fromString(String.valueOf(m.get("player-id")));
        String name = m.get("player-name") != null ? String.valueOf(m.get("player-name")) : "?";
        SmithProfile p = new SmithProfile(id, name);
        Object xpRaw = m.get("xp");
        p.xp = xpRaw == null ? 0L : Long.parseLong(String.valueOf(xpRaw));
        Object repRaw = m.get("reputation");
        p.reputation = repRaw == null ? 0 : Integer.parseInt(String.valueOf(repRaw));
        return p;
    }
}
