package com.nexuscraft.nexuscraftersguild;

/**
 * The three ESO-style "something's happening right now, somewhere" world events
 * {@link GuildEventScheduler} rolls between -- deliberately three different shapes of thing to do,
 * not three skins on the same fight:
 *
 * <p>{@link #RAIDER_INCURSION} -- pure combat. A tagged pack of hostiles spawns; clear it, everyone
 * who landed a hit gets paid the instant the last one falls.
 *
 * <p>{@link #RICH_VEIN} -- pure gathering, no combat at all. A chest of smithing materials appears
 * in the open, first player to reach it gets a finder's bonus on top of whatever's inside, and it's
 * gone again after its window closes.
 *
 * <p>{@link #SUPPLY_CARAVAN} -- both, with a clock running. A smaller tagged guard detail spawns
 * around a caravan; clear the guards to reveal its chest, but the whole thing (guards and chest
 * alike) still vanishes at the event's original deadline -- moving too slowly means missing the
 * loot even after winning the fight.
 */
enum EventType {
    RAIDER_INCURSION,
    RICH_VEIN,
    SUPPLY_CARAVAN
}
