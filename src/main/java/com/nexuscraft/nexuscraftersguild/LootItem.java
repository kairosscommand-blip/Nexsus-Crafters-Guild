package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Material;

/** One weighted entry in a loot pool -- see {@link GuildEventLootTable}. */
record LootItem(Material material, int min, int max, int weight) {
}
