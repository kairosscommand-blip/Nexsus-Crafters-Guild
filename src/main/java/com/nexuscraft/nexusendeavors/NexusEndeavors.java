package com.nexuscraft.nexusendeavors;

import com.nexuscraft.nexusendeavors.api.NexusEndeavorsApi;
import com.nexuscraft.nexusendeavors.api.NexusEndeavorsApiImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Guild's endeavor board -- daily/weekly objectives, a login streak, and Endeavor Seals
 * spendable at a cosmetics vendor, built to be the one always-there reason to log in and do
 * something, whether that something is generic (kill hostiles, mine ore, just play) or specific to
 * another Nexus plugin (see this plugin's new {@code api} package and the sibling EndeavorsBridge
 * classes added to NexusDungeons/NexusCraftersGuild/NexusHouses this same pass).
 */
public final class NexusEndeavors extends JavaPlugin {

    private ObjectivePool pool;
    private DailyBoard board;
    private EndeavorRegistry registry;
    private EndeavorProgressEngine engine;
    private CosmeticVendor vendor;
    private LoginStreakListener loginStreakListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EndeavorsKeys keys = new EndeavorsKeys(this);

        this.pool = new ObjectivePool(this);
        pool.load();
        this.board = new DailyBoard(this, pool);
        this.registry = new EndeavorRegistry(this);
        registry.load();
        this.engine = new EndeavorProgressEngine(this, pool, board, registry);

        this.vendor = new CosmeticVendor(this, keys);
        vendor.load();

        this.loginStreakListener = new LoginStreakListener(this, board, engine, registry);
        getServer().getPluginManager().registerEvents(loginStreakListener, this);
        getServer().getPluginManager().registerEvents(new StandaloneObjectiveListener(this, engine), this);
        getServer().getPluginManager().registerEvents(new VendorListener(vendor, engine, keys), this);

        getCommand("endeavors").setExecutor(new EndeavorsCommandExecutor(this, engine, vendor));

        long playtimeIntervalTicks = 20L * 60; // once a minute, real time -- see PlaytimeTracker
        Bukkit.getScheduler().runTaskTimer(this, new PlaytimeTracker(engine), playtimeIntervalTicks, playtimeIntervalTicks);

        long flushIntervalTicks = Math.max(1200, getConfig().getLong("persistence.flush-interval-ticks", 6000));
        Bukkit.getScheduler().runTaskTimer(this, registry::save, flushIntervalTicks, flushIntervalTicks);

        // Publishes reportProgress(...) for the rest of the Nexus family to call without a
        // compile-time dependency -- same soft Class.forName + ServicesManager pattern every
        // other cross-plugin surface in this project uses. See this plugin's api package.
        getServer().getServicesManager().register(NexusEndeavorsApi.class,
                new NexusEndeavorsApiImpl(engine), this, ServicePriority.Normal);

        getLogger().info("NexusEndeavors enabled. " + pool.dailyPool().size() + " daily and "
                + pool.weeklyPool().size() + " weekly objective(s) in the pool, " + vendor.all().size()
                + " vendor item(s).");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.save();
        }
        getLogger().info("NexusEndeavors disabled.");
    }

    void reloadAll() {
        reloadConfig();
        pool.load();
        vendor.load();
        board.refresh();
        loginStreakListener.reloadMilestones();
    }
}
