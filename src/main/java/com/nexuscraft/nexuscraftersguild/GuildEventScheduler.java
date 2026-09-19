package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * "Just something to do all the time," per the ask -- an Elder-Scrolls-Online-style always-on
 * ticker that periodically checks whether a new world event should start, picks a random online
 * player and a random point somewhere near (not on top of) them, and drops one of
 * {@link EventType}'s three flavors there, announced server-wide so anyone can go join in.
 * Deliberately never targets a specific player's base or claim -- always a fresh, anonymous
 * coordinate, the same spirit as a real ESO public event appearing "somewhere in the zone."
 */
final class GuildEventScheduler {

    private final JavaPlugin plugin;
    private final GuildEventRegistry registry;
    private final GuildEventLootTable lootTable;
    private final SmithRegistry smiths;
    private final EconomyBridge economy;
    private final GuildKeys keys;
    private final Random random = new Random();

    private final boolean enabled;
    private final int spawnChancePercent;
    private final int maxConcurrent;
    private final int minRadius;
    private final int maxRadius;

    private final int raiderMobMin;
    private final int raiderMobMax;
    private final double raiderHealthMult;
    private final double raiderDamageMult;
    private final long raiderDurationMillis;
    private final double raiderGold;
    private final long raiderXp;
    private final int raiderReputation;

    private final long richVeinDurationMillis;
    private final double richVeinFinderGold;
    private final long richVeinFinderXp;
    private final int richVeinFinderReputation;

    private final int caravanGuardMin;
    private final int caravanGuardMax;
    private final double caravanHealthMult;
    private final double caravanDamageMult;
    private final long caravanDurationMillis;
    private final double caravanClearGold;
    private final long caravanClearXp;
    private final int caravanClearReputation;

    private BukkitTask task;

    GuildEventScheduler(JavaPlugin plugin, GuildEventRegistry registry, GuildEventLootTable lootTable,
            SmithRegistry smiths, EconomyBridge economy, GuildKeys keys) {
        this.plugin = plugin;
        this.registry = registry;
        this.lootTable = lootTable;
        this.smiths = smiths;
        this.economy = economy;
        this.keys = keys;

        this.enabled = plugin.getConfig().getBoolean("events.enabled", true);
        this.spawnChancePercent = plugin.getConfig().getInt("events.spawn-chance-percent", 20);
        this.maxConcurrent = Math.max(1, plugin.getConfig().getInt("events.max-concurrent", 2));
        this.minRadius = Math.max(0, plugin.getConfig().getInt("events.min-radius", 40));
        this.maxRadius = Math.max(minRadius + 1, plugin.getConfig().getInt("events.max-radius", 150));

        this.raiderMobMin = Math.max(1, plugin.getConfig().getInt("events.raider.mob-count-min", 4));
        this.raiderMobMax = Math.max(raiderMobMin, plugin.getConfig().getInt("events.raider.mob-count-max", 7));
        this.raiderHealthMult = plugin.getConfig().getDouble("events.raider.health-multiplier", 1.5);
        this.raiderDamageMult = plugin.getConfig().getDouble("events.raider.damage-multiplier", 1.3);
        this.raiderDurationMillis = 1000L * Math.max(30, plugin.getConfig().getInt("events.raider.duration-seconds", 600));
        this.raiderGold = plugin.getConfig().getDouble("events.raider.gold-reward", 40);
        this.raiderXp = Math.max(0, plugin.getConfig().getInt("events.raider.xp-reward", 30));
        this.raiderReputation = Math.max(0, plugin.getConfig().getInt("events.raider.reputation-reward", 3));

        this.richVeinDurationMillis = 1000L * Math.max(30, plugin.getConfig().getInt("events.rich-vein.duration-seconds", 300));
        this.richVeinFinderGold = plugin.getConfig().getDouble("events.rich-vein.finder-gold-reward", 15);
        this.richVeinFinderXp = Math.max(0, plugin.getConfig().getInt("events.rich-vein.finder-xp-reward", 10));
        this.richVeinFinderReputation = Math.max(0, plugin.getConfig().getInt("events.rich-vein.finder-reputation-reward", 1));

        this.caravanGuardMin = Math.max(1, plugin.getConfig().getInt("events.caravan.guard-count-min", 2));
        this.caravanGuardMax = Math.max(caravanGuardMin, plugin.getConfig().getInt("events.caravan.guard-count-max", 4));
        this.caravanHealthMult = plugin.getConfig().getDouble("events.caravan.health-multiplier", 1.1);
        this.caravanDamageMult = plugin.getConfig().getDouble("events.caravan.damage-multiplier", 1.0);
        this.caravanDurationMillis = 1000L * Math.max(30, plugin.getConfig().getInt("events.caravan.duration-seconds", 420));
        this.caravanClearGold = plugin.getConfig().getDouble("events.caravan.clear-gold-reward", 25);
        this.caravanClearXp = Math.max(0, plugin.getConfig().getInt("events.caravan.clear-xp-reward", 20));
        this.caravanClearReputation = Math.max(0, plugin.getConfig().getInt("events.caravan.clear-reputation-reward", 2));
    }

    void start() {
        if (!enabled) {
            return;
        }
        long interval = Math.max(200, plugin.getConfig().getLong("events.check-interval-ticks", 1200));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    void stop() {
        // Real Bukkit: BukkitTask#cancel(). This stub's BukkitTask has no such method yet (nothing
        // in this project family has needed to cancel a repeating task before now) -- dropping the
        // reference is enough for this stub build; a real server cleanly stops it via cancel() on
        // plugin disable regardless (Bukkit unregisters every task owned by a disabling plugin),
        // so this is a compile-time-only gap, not a real-server one. See README.
        task = null;
    }

    private void tick() {
        expireStale();
        if (registry.activeCount() >= maxConcurrent) {
            return;
        }
        if (random.nextInt(100) >= spawnChancePercent) {
            return;
        }
        List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            return;
        }
        Player anchor = online.get(random.nextInt(online.size()));
        Location anchorLoc = anchor.getLocation();
        World world = anchorLoc.getWorld();
        if (world == null) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        int distance = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius));
        int x = anchorLoc.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = anchorLoc.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        int y = anchorLoc.getBlockY();

        EventType[] types = EventType.values();
        EventType type = types[random.nextInt(types.length)];
        switch (type) {
            case RAIDER_INCURSION -> spawnRaiderIncursion(world, x, y, z);
            case RICH_VEIN -> spawnRichVein(world, x, y, z);
            case SUPPLY_CARAVAN -> spawnSupplyCaravan(world, x, y, z);
        }
    }

    /** Sweeps every event past its deadline -- not just the still-ACTIVE ones. A COMPLETE Supply
     *  Caravan (guards cleared, chest revealed -- see completeCaravanClear/revealCaravanChest) is
     *  never re-added to "active," so if this only looked at registry.active() its chest would sit
     *  in the world forever once no longer ACTIVE; sweeping registry.all() instead means a cleared
     *  caravan's leftover chest is still cleaned up at the event's original deadline, exactly like
     *  EventType's own class comment promises, and every event (win, loss, or ignored) eventually
     *  leaves the registry instead of accumulating for the life of the server. */
    private void expireStale() {
        long now = System.currentTimeMillis();
        for (WorldEvent event : new ArrayList<>(registry.all())) {
            if (!event.isExpired(now)) {
                continue;
            }
            if (event.status == WorldEvent.Status.ACTIVE) {
                event.status = WorldEvent.Status.EXPIRED;
                Bukkit.broadcastMessage(Colors.color("&7The " + label(event.type)
                        + " near " + event.x + ", " + event.z + " has moved on."));
            }
            if (event.chestX != null && !event.chestLooted) {
                World world = Bukkit.getWorld(event.world);
                if (world != null) {
                    world.getBlockAt(event.chestX, event.chestY, event.chestZ).setType(Material.AIR);
                }
            }
            registry.remove(event.id);
        }
    }

    private void spawnRaiderIncursion(World world, int x, int y, int z) {
        UUID id = UUID.randomUUID();
        WorldEvent event = new WorldEvent(id, EventType.RAIDER_INCURSION, world.getName(), x, y, z,
                System.currentTimeMillis() + raiderDurationMillis);
        int count = raiderMobMin + random.nextInt(raiderMobMax - raiderMobMin + 1);
        event.mobsRemaining = count;
        EntityType[] pool = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.VINDICATOR, EntityType.WITCH};
        for (int i = 0; i < count; i++) {
            spawnTaggedMob(world, x, y, z, pool[random.nextInt(pool.length)], id, raiderHealthMult, raiderDamageMult);
        }
        registry.add(event);
        Bukkit.broadcastMessage(Colors.color("&c&lRaider Incursion! &r&cA band of raiders has appeared near "
                + x + ", " + z + " -- rally the Guild and drive them off for gold, guild standing, and smithing XP!"));
    }

    private void spawnRichVein(World world, int x, int y, int z) {
        UUID id = UUID.randomUUID();
        WorldEvent event = new WorldEvent(id, EventType.RICH_VEIN, world.getName(), x, y, z,
                System.currentTimeMillis() + richVeinDurationMillis);
        event.chestX = x;
        event.chestY = y;
        event.chestZ = z;
        placeAndFillChest(world, x, y, z, "rich-vein");
        registry.add(event);
        Bukkit.broadcastMessage(Colors.color("&e&lRich Vein! &r&eThe Guild has marked a rich seam of ore near "
                + x + ", " + z + " -- first to reach it keeps what it holds."));
    }

    private void spawnSupplyCaravan(World world, int x, int y, int z) {
        UUID id = UUID.randomUUID();
        WorldEvent event = new WorldEvent(id, EventType.SUPPLY_CARAVAN, world.getName(), x, y, z,
                System.currentTimeMillis() + caravanDurationMillis);
        int count = caravanGuardMin + random.nextInt(caravanGuardMax - caravanGuardMin + 1);
        event.mobsRemaining = count;
        EntityType[] pool = {EntityType.ZOMBIE, EntityType.SKELETON};
        for (int i = 0; i < count; i++) {
            spawnTaggedMob(world, x, y, z, pool[random.nextInt(pool.length)], id, caravanHealthMult, caravanDamageMult);
        }
        registry.add(event);
        Bukkit.broadcastMessage(Colors.color("&b&lSupply Caravan! &r&bA guarded caravan is passing near "
                + x + ", " + z + " -- clear its guards before it moves on to claim its goods."));
    }

    private void spawnTaggedMob(World world, int x, int y, int z, EntityType type, UUID eventId,
            double healthMult, double damageMult) {
        Entity spawned = world.spawnEntity(new Location(world, x, y, z), type);
        if (!(spawned instanceof LivingEntity mob)) {
            return;
        }
        mob.getPersistentDataContainer().set(keys.eventMobId, GuildKeys.STRING, eventId.toString());
        mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(mob.getAttribute(Attribute.MAX_HEALTH).getBaseValue() * healthMult);
        mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getValue());
        mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(mob.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue() * damageMult);
        mob.setCustomName(Colors.color("&6Guild Event: " + prettyName(type)));
        mob.setCustomNameVisible(true);
    }

    /** Called by GuildEventListener once a Supply Caravan's last guard falls. */
    void revealCaravanChest(WorldEvent event) {
        World world = Bukkit.getWorld(event.world);
        if (world == null) {
            return;
        }
        event.chestX = event.x;
        event.chestY = event.y;
        event.chestZ = event.z;
        placeAndFillChest(world, event.x, event.y, event.z, "caravan");
    }

    private void placeAndFillChest(World world, int x, int y, int z, String pool) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            for (ItemStack item : lootTable.roll(pool, 3, 6, random)) {
                chest.getInventory().addItem(item);
            }
        }
    }

    // ---- rewards, called by GuildEventListener on completion ----

    void completeRaiderIncursion(WorldEvent event) {
        payParticipants(event, raiderGold, raiderXp, raiderReputation);
        Bukkit.broadcastMessage(Colors.color("&a&lRaider Incursion cleared! &r&aThe Guild thanks everyone who answered the call."));
    }

    void completeCaravanClear(WorldEvent event) {
        payParticipants(event, caravanClearGold, caravanClearXp, caravanClearReputation);
        Bukkit.broadcastMessage(Colors.color("&a&lThe caravan's guards have fallen! &r&aIts goods are there for the taking."));
        revealCaravanChest(event);
    }

    void awardRichVeinFinder(Player finder) {
        rewardPlayer(finder, richVeinFinderGold, richVeinFinderXp, richVeinFinderReputation);
    }

    void awardCaravanFinder(Player finder) {
        // Looting the revealed caravan chest is its own small bonus on top of the clear-reward
        // every participant already got -- same finder's-bonus shape as Rich Vein.
        rewardPlayer(finder, richVeinFinderGold, richVeinFinderXp, richVeinFinderReputation);
    }

    private void payParticipants(WorldEvent event, double gold, long xp, int reputation) {
        for (UUID participantId : event.participantIds) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant == null) {
                continue; // offline by the time the event finished -- see README's honest limitation
            }
            rewardPlayer(participant, gold, xp, reputation);
        }
    }

    private void rewardPlayer(Player player, double gold, long xp, int reputation) {
        if (gold > 0) {
            economy.deposit(player, gold);
        }
        SmithProfile profile = smiths.getOrCreate(player.getUniqueId(), player.getName());
        if (xp > 0) {
            smiths.addXp(profile, xp);
        }
        if (reputation > 0) {
            smiths.addReputation(profile, reputation);
        }
        player.sendMessage(Colors.color("&6The Crafters Guild pays out: &f" + economy.format(gold)
                + " &6, &f+" + xp + " smithing xp&6, &f+" + reputation + " reputation&6."));
    }

    private static String prettyName(EntityType type) {
        String[] parts = type.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String label(EventType type) {
        return switch (type) {
            case RAIDER_INCURSION -> "raider camp";
            case RICH_VEIN -> "rich vein";
            case SUPPLY_CARAVAN -> "supply caravan";
        };
    }
}
