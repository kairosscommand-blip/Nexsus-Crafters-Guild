package com.nexuscraft.nexuscraftersguild;

import java.util.UUID;

/**
 * One player's standing with the Crafters Guild. Two separate numbers on purpose, so the two
 * halves of this plugin reward two different kinds of effort instead of collapsing into one grind:
 *
 * <p><b>Level/XP</b> comes from crafted OUTPUT at a forge you own -- every item CrafterCraftListener
 * upgrades adds XP, whether that Crafter block was triggered by your own hand or by a hopper clock
 * you built and walked away from (see CrafterCraftListener's own comment on why this plugin can't
 * reliably tell those apart, and chose not to pretend it can). Level raises your quality-tier
 * ceiling -- a level 3 smith's forge cannot rolls Legendary gear no matter how many Crafters feed
 * it, so a redstone clock alone still tops out well below what a genuinely leveled smith can reach.
 *
 * <p><b>Reputation</b> comes only from things a machine can't do for you: fulfilling a customer's
 * forge tip, completing a Guild Contract, pulling your weight in a world event. It's the number
 * that actually reflects "this player is known and trusted," and it's what future versions of the
 * forge-listing/perks system (see README roadmap) are meant to gate on, deliberately kept separate
 * from raw production volume.
 */
final class SmithProfile {

    final UUID playerId;
    String playerName;
    long xp;
    int reputation;

    SmithProfile(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    int level() {
        return SmithLeveling.levelForXp(xp);
    }

    String title() {
        return SmithLeveling.titleForLevel(level());
    }
}
