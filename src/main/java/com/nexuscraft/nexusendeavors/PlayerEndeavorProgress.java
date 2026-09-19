package com.nexuscraft.nexusendeavors;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One player's standing with the Guild's endeavor board -- progress toward the currently-active
 *  daily/weekly objectives, their Endeavor Seals balance, and their login streak. */
final class PlayerEndeavorProgress {

    final UUID playerId;
    String playerName;

    final Map<String, Integer> dailyProgress = new LinkedHashMap<>();
    final Set<String> dailyCompleted = new LinkedHashSet<>();
    long dailyResetDayIndex = Long.MIN_VALUE;

    final Map<String, Integer> weeklyProgress = new LinkedHashMap<>();
    final Set<String> weeklyCompleted = new LinkedHashSet<>();
    long weeklyResetWeekIndex = Long.MIN_VALUE;

    long seals;
    long lifetimeSealsEarned;

    int loginStreak;
    long lastLoginDayIndex = Long.MIN_VALUE;

    PlayerEndeavorProgress(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }
}
