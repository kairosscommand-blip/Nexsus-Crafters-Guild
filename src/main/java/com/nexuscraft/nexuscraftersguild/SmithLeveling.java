package com.nexuscraft.nexuscraftersguild;

/**
 * The XP curve and level-to-title mapping, pulled into its own small stateless class rather than
 * buried in SmithProfile/SmithRegistry, since both CrafterCraftListener (checking a quality-tier
 * ceiling) and the /guild profile command (showing "X xp to next level") need the same math and
 * shouldn't be able to drift apart.
 *
 * <p>Deliberately a smooth curve rather than a hardcoded per-level table: {@code xpForLevel(n)}
 * grows roughly with {@code n^1.6}, so early levels come fast (a new smith feels progress within
 * their first few dozen crafted items) and late levels take real, sustained effort -- Grandmaster
 * is meant to mean something.
 */
final class SmithLeveling {

    static final int MAX_LEVEL = 40;

    private SmithLeveling() {
    }

    /** Total cumulative XP required to REACH this level (level 1 requires 0). */
    static long xpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        return Math.round(35.0 * Math.pow(level, 1.6));
    }

    static int levelForXp(long xp) {
        int level = 1;
        while (level < MAX_LEVEL && xp >= xpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    static long xpToNextLevel(long xp) {
        int level = levelForXp(xp);
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return xpForLevel(level + 1) - xp;
    }

    static String titleForLevel(int level) {
        if (level >= 35) {
            return "Grandmaster Smith";
        }
        if (level >= 20) {
            return "Master Smith";
        }
        if (level >= 10) {
            return "Artisan Smith";
        }
        if (level >= 5) {
            return "Journeyman Smith";
        }
        return "Apprentice Smith";
    }
}
