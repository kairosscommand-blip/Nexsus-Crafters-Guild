package com.nexuscraft.nexusendeavors;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Which objectives are "today's"/"this week's" -- the SAME set for every player, deliberately
 * (per the "log in and do something" ask: everyone logging in today sees the same board, the same
 * way a real ESO daily/weekly endeavor rotation is shared server-wide, not personalized). Picked
 * deterministically from a day/week index and {@link ObjectivePool}'s current pool with a
 * {@link Random} seeded by that index -- so the same day always rotates to the same subset (no
 * state needs to be saved for "what's active today," it's recomputed identically by every check),
 * but a different day almost always rotates to a different subset.
 *
 * <p>The day/week boundary is UTC by default; {@code reset.utc-offset-hours} shifts it to whatever
 * local "midnight" the server actually wants (see README).
 */
final class DailyBoard {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long WEEK_MILLIS = 7 * DAY_MILLIS;

    private final JavaPlugin plugin;
    private final ObjectivePool pool;
    private int dailyActiveCount;
    private int weeklyActiveCount;
    private long offsetMillis;

    DailyBoard(JavaPlugin plugin, ObjectivePool pool) {
        this.plugin = plugin;
        this.pool = pool;
        refresh();
    }

    /** Re-reads daily-active-count/weekly-active-count/reset.utc-offset-hours from config.yml --
     *  called from NexusEndeavors#reloadAll() so {@code /endeavors reload} actually applies these
     *  instead of only taking effect on the next full plugin startup. Note this only changes how
     *  many objectives get PICKED and where the day boundary falls going forward -- it doesn't
     *  retroactively reshuffle which objectives were already active today (see README). */
    void refresh() {
        this.dailyActiveCount = Math.max(1, plugin.getConfig().getInt("daily-active-count", 3));
        this.weeklyActiveCount = Math.max(1, plugin.getConfig().getInt("weekly-active-count", 2));
        this.offsetMillis = plugin.getConfig().getInt("reset.utc-offset-hours", 0) * 3600_000L;
    }

    long dayIndex(long nowMillis) {
        return Math.floorDiv(nowMillis + offsetMillis, DAY_MILLIS);
    }

    long weekIndex(long nowMillis) {
        return Math.floorDiv(nowMillis + offsetMillis, WEEK_MILLIS);
    }

    List<ObjectiveDefinition> activeDaily(long nowMillis) {
        return pickActive(pool.dailyPool(), dailyActiveCount, dayIndex(nowMillis), "daily");
    }

    List<ObjectiveDefinition> activeWeekly(long nowMillis) {
        return pickActive(pool.weeklyPool(), weeklyActiveCount, weekIndex(nowMillis), "weekly");
    }

    private List<ObjectiveDefinition> pickActive(List<ObjectiveDefinition> source, int count, long seedIndex, String salt) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<ObjectiveDefinition> shuffled = new ArrayList<>(source);
        // salt.hashCode() keeps the daily and weekly rotations independent even though they'd
        // otherwise share a similar-looking seed on the day a week boundary and a day boundary
        // happen to land close together.
        Collections.shuffle(shuffled, new Random(seedIndex * 1_000_003L + salt.hashCode()));
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
