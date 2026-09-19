package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Guild Contracts board -- standing, always-available turn-ins loaded from {@code contracts:}
 * in config.yml (same defensive-per-entry-parsing convention as every loot table in this family: a
 * malformed contract is skipped with a warning, never fatal to the rest of the board). Unlike a
 * world event, a contract is never time-limited or exclusive -- it's the guild's steady, everyday
 * work, always there for whoever wants it, which is exactly what keeps the "always something to
 * do" feeling alive between world-event spawns.
 */
final class ContractBoard {

    private final JavaPlugin plugin;
    private final Map<String, GuildContractTemplate> contracts = new LinkedHashMap<>();

    ContractBoard(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        contracts.clear();
        for (Map<?, ?> raw : plugin.getConfig().getMapList("contracts")) {
            GuildContractTemplate contract = parse(raw);
            if (contract != null) {
                contracts.put(contract.id().toLowerCase(java.util.Locale.ROOT), contract);
            }
        }
    }

    private GuildContractTemplate parse(Map<?, ?> raw) {
        Object idObj = raw.get("id");
        Object materialObj = raw.get("material");
        Material material = materialObj != null ? Material.matchMaterial(String.valueOf(materialObj)) : null;
        if (idObj == null || material == null) {
            plugin.getLogger().warning("[NexusCraftersGuild] Skipping a malformed contract entry (bad/missing id or material): " + raw);
            return null;
        }
        int amount = intOf(raw, "amount", 0);
        if (amount <= 0) {
            plugin.getLogger().warning("[NexusCraftersGuild] Skipping a malformed contract entry '" + idObj + "' (bad amount): " + raw);
            return null;
        }
        String description = raw.get("description") != null ? String.valueOf(raw.get("description"))
                : "Bring " + amount + " " + material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        double gold = doubleOf(raw, "gold-reward", 0);
        long xp = intOf(raw, "xp-reward", 0);
        int reputation = intOf(raw, "reputation-reward", 0);
        return new GuildContractTemplate(String.valueOf(idObj), description, material, amount, gold, xp, reputation);
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

    private double doubleOf(Map<?, ?> raw, String key, double def) {
        Object v = raw.get(key);
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    List<GuildContractTemplate> all() {
        return new ArrayList<>(contracts.values());
    }

    GuildContractTemplate byId(String id) {
        return id == null ? null : contracts.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    int countHeld(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** Removes exactly this many of the material from the player's inventory. Caller must have
     *  already confirmed (via {@link #countHeld}) that the player holds at least this many. */
    void remove(Player player, Material material, int amount) {
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inventory.getContents().length && remaining > 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() != material) {
                continue;
            }
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            remaining -= take;
            inventory.setItem(i, item.getAmount() <= 0 ? null : item);
        }
    }
}
