package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Material;

/**
 * One standing Guild Contract -- "bring the Guild N of this material, get paid." Deliberately
 * material+amount rather than tier-specific ("a Fine sword or better"): keeping the requirement
 * plain-vanilla-Material means a brand new player with no forge of their own yet can still
 * participate by mining/farming/trading for the raw goods, not just an established smith turning in
 * their own gear -- contracts are the guild's other front door, alongside the forge.
 */
record GuildContractTemplate(String id, String description, Material material, int amount,
        double goldReward, long xpReward, int reputationReward) {
}
