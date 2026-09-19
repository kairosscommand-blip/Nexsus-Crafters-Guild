package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists every claimed forge to forges.yml -- same flat list-of-maps pattern as every other
 * registry in this family (a forge's name is free text, never a YAML path segment). The
 * per-window XP-throttle fields on {@link Forge} are transient and deliberately never written
 * here -- they reset to zero on every reload/restart, which just means a freshly-restarted
 * server's forges get one full unthrottled minute before the cap kicks back in; harmless.
 */
final class ForgeRegistry {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Forge> forgesById = new LinkedHashMap<>();

    ForgeRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "forges.yml");
    }

    void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        forgesById.clear();
        for (Map<?, ?> raw : data.getMapList("forges")) {
            try {
                Forge forge = fromMap(raw);
                forgesById.put(forge.id, forge);
            } catch (Exception e) {
                plugin.getLogger().warning("[NexusCraftersGuild] Skipping a corrupt forge entry: " + e.getMessage());
            }
        }
    }

    void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Forge forge : forgesById.values()) {
            list.add(toMap(forge));
        }
        YamlConfiguration data = new YamlConfiguration();
        data.set("forges", list);
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[NexusCraftersGuild] Could not save forges.yml: " + e.getMessage());
        }
    }

    void add(Forge forge) {
        forgesById.put(forge.id, forge);
        save();
    }

    boolean removeByName(String name) {
        Forge match = byName(name);
        if (match == null) {
            return false;
        }
        forgesById.remove(match.id);
        save();
        return true;
    }

    Forge byName(String name) {
        for (Forge forge : forgesById.values()) {
            if (forge.name.equalsIgnoreCase(name)) {
                return forge;
            }
        }
        return null;
    }

    Collection<Forge> all() {
        return forgesById.values();
    }

    Collection<Forge> byOwner(UUID ownerId) {
        List<Forge> owned = new ArrayList<>();
        for (Forge forge : forgesById.values()) {
            if (forge.ownerId.equals(ownerId)) {
                owned.add(forge);
            }
        }
        return owned;
    }

    /** The forge whose claimed circle contains this location, or null -- checked on every
     *  CrafterCraftEvent, so this stays a plain linear scan rather than a spatial index (same
     *  reasoning DungeonRegistry's own dungeonContaining() documents: not this plugin's expected
     *  scale). Two overlapping claims shouldn't normally exist (a natural next step is rejecting an
     *  overlapping /guild forge claim outright -- not yet built this pass, see README). */
    Forge forgeAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String worldName = location.getWorld().getName();
        for (Forge forge : forgesById.values()) {
            if (forge.contains(worldName, location.getX(), location.getY(), location.getZ())) {
                return forge;
            }
        }
        return null;
    }

    private static Map<String, Object> toMap(Forge f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.id.toString());
        m.put("name", f.name);
        m.put("owner-id", f.ownerId.toString());
        m.put("owner-name", f.ownerName);
        m.put("world", f.world);
        m.put("x", f.x);
        m.put("y", f.y);
        m.put("z", f.z);
        m.put("radius", f.radius);
        m.put("items-forged", f.itemsForged);
        return m;
    }

    private static Forge fromMap(Map<?, ?> m) {
        UUID id = UUID.fromString(String.valueOf(m.get("id")));
        String name = String.valueOf(m.get("name"));
        UUID ownerId = UUID.fromString(String.valueOf(m.get("owner-id")));
        String ownerName = m.get("owner-name") != null ? String.valueOf(m.get("owner-name")) : "?";
        String world = String.valueOf(m.get("world"));
        Forge f = new Forge(id, name, ownerId, ownerName, world,
                intOf(m, "x"), intOf(m, "y"), intOf(m, "z"), intOf(m, "radius"));
        Object itemsRaw = m.get("items-forged");
        f.itemsForged = itemsRaw == null ? 0L : Long.parseLong(String.valueOf(itemsRaw));
        return f;
    }

    private static int intOf(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? 0 : Integer.parseInt(String.valueOf(v));
    }
}
