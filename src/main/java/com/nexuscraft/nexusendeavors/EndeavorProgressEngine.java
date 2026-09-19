package com.nexuscraft.nexusendeavors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * The heart of the plugin: routes a progress report (from this plugin's own standalone listeners,
 * or from another Nexus plugin's EndeavorsBridge via {@link com.nexuscraft.nexusendeavors.api.NexusEndeavorsApiImpl})
 * against whichever daily/weekly objectives are currently active and share that objective key,
 * handles the daily/weekly rollover, and pays out Endeavor Seals the moment an objective completes.
 */
public final class EndeavorProgressEngine {

    private final JavaPlugin plugin;
    private final ObjectivePool pool;
    private final DailyBoard board;
    private final EndeavorRegistry registry;

    EndeavorProgressEngine(JavaPlugin plugin, ObjectivePool pool, DailyBoard board, EndeavorRegistry registry) {
        this.plugin = plugin;
        this.pool = pool;
        this.board = board;
        this.registry = registry;
    }

    public void reportProgress(UUID playerId, String playerName, String objectiveKey, int amount) {
        if (playerId == null || objectiveKey == null || amount <= 0) {
            return;
        }
        PlayerEndeavorProgress profile = registry.getOrCreate(playerId, playerName);
        ensureCurrent(profile);

        boolean anyMatched = false;
        for (ObjectiveDefinition def : board.activeDaily(System.currentTimeMillis())) {
            if (def.objectiveKey().equals(objectiveKey)) {
                anyMatched = true;
                applyProgress(profile, def, profile.dailyProgress, profile.dailyCompleted, amount);
            }
        }
        for (ObjectiveDefinition def : board.activeWeekly(System.currentTimeMillis())) {
            if (def.objectiveKey().equals(objectiveKey)) {
                anyMatched = true;
                applyProgress(profile, def, profile.weeklyProgress, profile.weeklyCompleted, amount);
            }
        }
        if (!anyMatched) {
            return; // nothing currently on the board cares about this key -- quietly discarded
        }
    }

    private void applyProgress(PlayerEndeavorProgress profile, ObjectiveDefinition def,
            java.util.Map<String, Integer> progressMap, java.util.Set<String> completedSet, int amount) {
        if (completedSet.contains(def.id())) {
            return; // already turned in for this cycle -- extra progress doesn't overflow into anything
        }
        int before = progressMap.getOrDefault(def.id(), 0);
        int after = Math.min(def.target(), before + amount);
        progressMap.put(def.id(), after);
        if (after >= def.target()) {
            completedSet.add(def.id());
            profile.seals += def.rewardSeals();
            profile.lifetimeSealsEarned += def.rewardSeals();
            Player player = Bukkit.getPlayer(profile.playerId);
            if (player != null) {
                player.sendMessage(Colors.color("&6&lEndeavor complete! &r&6" + def.description()
                        + " &7-- &f+" + def.rewardSeals() + " Endeavor Seals&7 (balance: " + profile.seals + ")"));
            }
        }
    }

    /** Rolls a player's daily/weekly progress over the moment they touch anything after a
     *  boundary has passed -- there's no global "midnight tick" scanning every known player; each
     *  profile just checks itself against the current index the first time it's touched after the
     *  rollover, which is exactly as correct and far cheaper on a server with more history than
     *  active players. */
    void ensureCurrent(PlayerEndeavorProgress profile) {
        long now = System.currentTimeMillis();
        long dayIndex = board.dayIndex(now);
        if (profile.dailyResetDayIndex != dayIndex) {
            profile.dailyProgress.clear();
            profile.dailyCompleted.clear();
            profile.dailyResetDayIndex = dayIndex;
        }
        long weekIndex = board.weekIndex(now);
        if (profile.weeklyResetWeekIndex != weekIndex) {
            profile.weeklyProgress.clear();
            profile.weeklyCompleted.clear();
            profile.weeklyResetWeekIndex = weekIndex;
        }
    }

    List<ObjectiveDefinition> activeDaily() {
        return board.activeDaily(System.currentTimeMillis());
    }

    List<ObjectiveDefinition> activeWeekly() {
        return board.activeWeekly(System.currentTimeMillis());
    }

    ObjectivePool pool() {
        return pool;
    }

    EndeavorRegistry registry() {
        return registry;
    }

    boolean spend(UUID playerId, long amount) {
        PlayerEndeavorProgress profile = registry.find(playerId);
        if (profile == null || amount <= 0 || profile.seals < amount) {
            return false;
        }
        profile.seals -= amount;
        return true;
    }

    /** Undoes a spend() that didn't actually deliver what it was paying for (see VendorListener's
     *  full-inventory case) -- deliberately does NOT touch lifetimeSealsEarned, since a refunded
     *  purchase was never really "earned" twice. */
    void refund(UUID playerId, long amount) {
        PlayerEndeavorProgress profile = registry.find(playerId);
        if (profile != null && amount > 0) {
            profile.seals += amount;
        }
    }
}
