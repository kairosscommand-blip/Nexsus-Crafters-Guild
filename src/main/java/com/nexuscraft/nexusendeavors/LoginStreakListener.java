package com.nexuscraft.nexusendeavors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.TreeMap;

/**
 * The actual "log in and do something" moment: every join counts a day toward (or resets) a login
 * streak, pays out escalating milestone bonuses, and immediately shows the player what's on today's
 * board -- so the very first thing anyone sees after connecting is a reason to go do something,
 * not something they have to remember to go check for themselves.
 */
final class LoginStreakListener implements Listener {

    private final JavaPlugin plugin;
    private final DailyBoard board;
    private final EndeavorProgressEngine engine;
    private final EndeavorRegistry registry;
    private final TreeMap<Integer, Long> milestones = new TreeMap<>();

    LoginStreakListener(JavaPlugin plugin, DailyBoard board, EndeavorProgressEngine engine, EndeavorRegistry registry) {
        this.plugin = plugin;
        this.board = board;
        this.engine = engine;
        this.registry = registry;
        reloadMilestones();
    }

    /** Re-reads streak-milestones from config.yml -- called from NexusEndeavors#reloadAll() so
     *  {@code /endeavors reload} actually picks up milestone changes instead of only affecting new
     *  plugin startups. */
    void reloadMilestones() {
        milestones.clear();
        for (Map<?, ?> raw : plugin.getConfig().getMapList("streak-milestones")) {
            Object dayObj = raw.get("day");
            Object sealsObj = raw.get("seals");
            if (dayObj == null || sealsObj == null) {
                continue;
            }
            try {
                milestones.put(Integer.parseInt(String.valueOf(dayObj)), Long.parseLong(String.valueOf(sealsObj)));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[NexusEndeavors] Skipping a malformed streak-milestones entry: " + raw);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        PlayerEndeavorProgress profile = registry.getOrCreate(player.getUniqueId(), player.getName());
        engine.ensureCurrent(profile);

        long today = board.dayIndex(System.currentTimeMillis());
        boolean streakAdvancedToday;
        if (profile.lastLoginDayIndex == today) {
            // already counted today (a relog/reconnect) -- no change, just show the board again.
            // Deliberately NOT treated as an advance: otherwise a same-day relog would re-trigger
            // whatever milestone reward is tied to the current streak length, over and over.
            streakAdvancedToday = false;
        } else if (profile.lastLoginDayIndex == today - 1) {
            profile.loginStreak++;
            streakAdvancedToday = true;
        } else {
            profile.loginStreak = 1;
            streakAdvancedToday = true;
        }
        profile.lastLoginDayIndex = today;

        if (streakAdvancedToday) {
            Long milestoneReward = milestones.get(profile.loginStreak);
            if (milestoneReward != null) {
                profile.seals += milestoneReward;
                profile.lifetimeSealsEarned += milestoneReward;
                player.sendMessage(Colors.color("&6&lLogin streak milestone! &r&6Day " + profile.loginStreak
                        + " in a row -- &f+" + milestoneReward + " Endeavor Seals&6!"));
            }
        }

        player.sendMessage(Colors.color("&6&lThe Crafters Guild's board &r&6-- day " + profile.loginStreak
                + " login streak, " + profile.seals + " Endeavor Seals."));
        for (ObjectiveDefinition def : engine.activeDaily()) {
            int progress = profile.dailyProgress.getOrDefault(def.id(), 0);
            boolean done = profile.dailyCompleted.contains(def.id());
            player.sendMessage(Colors.color((done ? "&a  ✓ " : "&7  - ") + "&f" + def.description()
                    + " &7(" + progress + "/" + def.target() + ")" + (done ? " &a[complete]" : "")));
        }
        player.sendMessage(Colors.color("&7Type &f/endeavors board &7to see this any time, or &f/endeavors vendor &7to spend your Seals."));
    }
}
