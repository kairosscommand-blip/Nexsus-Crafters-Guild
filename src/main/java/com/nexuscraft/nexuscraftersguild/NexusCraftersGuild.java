package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Crafters Guild -- a real blacksmithing profession built around the vanilla Auto Crafter
 * block, and an ESO-style "always something happening" world-events/contracts layer, deliberately
 * built as one plugin rather than two: see README for the full story of why. In short, both halves
 * feed the same two numbers (a smith's level and their guild reputation), so hammering out gear at
 * your forge and answering a Raider Incursion call across the map both visibly grow the same
 * standing, instead of being two unrelated systems that happen to share a jar file.
 */
public final class NexusCraftersGuild extends JavaPlugin {

    private GuildKeys keys;
    private SmithRegistry smiths;
    private ForgeRegistry forges;
    private ContractBoard contracts;
    private GuildEventRegistry events;
    private GuildEventLootTable eventLootTable;
    private EconomyBridge economy;
    private GuildEventScheduler eventScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.keys = new GuildKeys(this);
        this.economy = new EconomyBridge();

        this.smiths = new SmithRegistry(this);
        smiths.load();

        this.forges = new ForgeRegistry(this);
        forges.load();

        this.contracts = new ContractBoard(this);
        contracts.load();

        this.eventLootTable = new GuildEventLootTable(this);
        eventLootTable.load();
        this.events = new GuildEventRegistry();

        this.eventScheduler = new GuildEventScheduler(this, events, eventLootTable, smiths, economy, keys);

        getServer().getPluginManager().registerEvents(new CrafterCraftListener(this, forges, smiths, keys), this);
        getServer().getPluginManager().registerEvents(new GuildEventListener(events, eventScheduler, keys), this);

        getCommand("guild").setExecutor(new GuildCommandExecutor(this, smiths, forges, contracts, events, economy));

        eventScheduler.start();

        long flushIntervalTicks = Math.max(1200, getConfig().getLong("persistence.flush-interval-ticks", 6000));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            smiths.save();
            forges.save();
        }, flushIntervalTicks, flushIntervalTicks);

        getLogger().info("NexusCraftersGuild enabled. " + forges.all().size() + " forge(s) and "
                + contracts.all().size() + " standing contract(s) known. Vault economy: "
                + (economy.isVaultConnected() ? "connected." : "not connected -- gold rewards won't pay out."));
    }

    @Override
    public void onDisable() {
        if (eventScheduler != null) {
            eventScheduler.stop();
        }
        if (smiths != null) {
            smiths.save();
        }
        if (forges != null) {
            forges.save();
        }
        getLogger().info("NexusCraftersGuild disabled.");
    }

    /** Reloads config.yml and re-parses the two lists that are read fresh every time regardless
     *  (Guild Contracts and world-event loot pools -- both plain data, safe to swap live). Every
     *  numeric tuning value elsewhere (smithing XP rates, forge radius limits, world-event timing/
     *  rewards) was read once into each listener/scheduler's own fields back in onEnable() and
     *  stays fixed until an actual restart -- same "reload only reaches what's safe to reach live"
     *  honesty as NexusDungeons' own reloadAll() (its generation settings only apply to dungeons
     *  created after the reload); see README. */
    void reloadAll() {
        reloadConfig();
        contracts.load();
        eventLootTable.load();
    }
}
