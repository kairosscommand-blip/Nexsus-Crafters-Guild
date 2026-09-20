package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Every player-facing and admin command this plugin has, under one root -- same one-root-command-
 *  with-subcommands shape as every other Nexus plugin (/house, /dungeons, ...). */
final class GuildCommandExecutor implements CommandExecutor {

    private final NexusCraftersGuild plugin;
    private final SmithRegistry smiths;
    private final ForgeRegistry forges;
    private final ContractBoard contracts;
    private final GuildEventRegistry events;
    private final EconomyBridge economy;

    private final int defaultForgeRadius;
    private final int maxForgeRadius;
    private final int maxForgesPerPlayer;

    GuildCommandExecutor(NexusCraftersGuild plugin, SmithRegistry smiths, ForgeRegistry forges,
            ContractBoard contracts, GuildEventRegistry events, EconomyBridge economy) {
        this.plugin = plugin;
        this.smiths = smiths;
        this.forges = forges;
        this.contracts = contracts;
        this.events = events;
        this.economy = economy;
        this.defaultForgeRadius = Math.max(1, plugin.getConfig().getInt("forge.default-radius", 12));
        this.maxForgeRadius = Math.max(defaultForgeRadius, plugin.getConfig().getInt("forge.max-radius", 32));
        this.maxForgesPerPlayer = Math.max(1, plugin.getConfig().getInt("forge.max-per-player", 1));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "profile" -> handleProfile(sender, args);
            case "forge" -> handleForge(sender, args);
            case "contracts" -> handleContracts(sender, args);
            case "events" -> handleEvents(sender);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    // ---- profile ----

    private void handleProfile(CommandSender sender, String[] args) {
        UUID targetId;
        String targetName;
        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            targetId = target.getUniqueId();
            targetName = target.getName() != null ? target.getName() : args[1];
        } else if (sender instanceof Player player) {
            targetId = player.getUniqueId();
            targetName = player.getName();
        } else {
            sender.sendMessage(color("&6Usage: &f/guild profile <player>"));
            return;
        }
        SmithProfile profile = smiths.find(targetId);
        if (profile == null) {
            sender.sendMessage(color("&7" + targetName + " hasn't done any work for the Guild yet."));
            return;
        }
        long toNext = SmithLeveling.xpToNextLevel(profile.xp);
        sender.sendMessage(color("&6" + targetName + " &7-- " + profile.title() + " (level " + profile.level() + ")"));
        sender.sendMessage(color("&7XP: &f" + profile.xp + (toNext > 0 ? " &7(" + toNext + " to next level)" : " &7(max level)")));
        sender.sendMessage(color("&7Guild reputation: &f" + profile.reputation));
    }

    // ---- forge ----

    private void handleForge(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&6Usage: &f/guild forge <claim|unclaim|list|info|tip>"));
            return;
        }
        String forgeSub = args[1].toLowerCase();
        switch (forgeSub) {
            case "claim" -> handleForgeClaim(sender, args);
            case "unclaim" -> handleForgeUnclaim(sender, args);
            case "list" -> handleForgeList(sender);
            case "info" -> handleForgeInfo(sender, args);
            case "tip" -> handleForgeTip(sender, args);
            default -> sender.sendMessage(color("&6Usage: &f/guild forge <claim|unclaim|list|info|tip>"));
        }
    }

    private void handleForgeClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly a player can do that (a forge is claimed around where you stand)."));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(color("&6Usage: &f/guild forge claim <name> [radius]"));
            return;
        }
        String name = args[2];
        if (forges.byName(name) != null) {
            player.sendMessage(color("&cA forge by that name already exists."));
            return;
        }
        if (forges.byOwner(player.getUniqueId()).size() >= maxForgesPerPlayer) {
            player.sendMessage(color("&cYou already have the maximum number of forges this server allows ("
                    + maxForgesPerPlayer + ")."));
            return;
        }
        int radius = defaultForgeRadius;
        if (args.length >= 4) {
            try {
                radius = Math.max(1, Math.min(maxForgeRadius, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                player.sendMessage(color("&cRadius must be a number."));
                return;
            }
        }
        Location origin = player.getLocation();
        Forge forge = new Forge(UUID.randomUUID(), name, player.getUniqueId(), player.getName(),
                origin.getWorld().getName(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius);
        forges.add(forge);
        smiths.getOrCreate(player.getUniqueId(), player.getName());
        player.sendMessage(color("&6Claimed &f" + name + " &6as your forge -- a " + radius
                + "-block radius. Any real Crafter block you place in range is now yours to smith with,"
                + " by hand or by automation."));
    }

    private void handleForgeUnclaim(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&6Usage: &f/guild forge unclaim <name>"));
            return;
        }
        Forge forge = forges.byName(args[2]);
        if (forge == null) {
            sender.sendMessage(color("&cNo forge by that name."));
            return;
        }
        boolean owner = sender instanceof Player p && p.getUniqueId().equals(forge.ownerId);
        if (!owner && !sender.hasPermission("nexuscraftersguild.admin")) {
            sender.sendMessage(color("&cOnly that forge's owner (or an admin) can unclaim it."));
            return;
        }
        forges.removeByName(args[2]);
        sender.sendMessage(color("&7Forge " + args[2] + " unclaimed. Any Crafter blocks left standing there are"
                + " now ordinary, un-smithing Crafters again."));
    }

    private void handleForgeList(CommandSender sender) {
        if (forges.all().isEmpty()) {
            sender.sendMessage(color("&7No forges have been claimed yet."));
            return;
        }
        sender.sendMessage(color("&6Claimed forges:"));
        for (Forge forge : forges.all()) {
            sender.sendMessage(color("&7- &f" + forge.name + " &7(owner: &f" + forge.ownerName
                    + "&7, " + forge.itemsForged + " item(s) forged)"));
        }
    }

    private void handleForgeInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&6Usage: &f/guild forge info <name>"));
            return;
        }
        Forge forge = forges.byName(args[2]);
        if (forge == null) {
            sender.sendMessage(color("&cNo forge by that name."));
            return;
        }
        SmithProfile owner = smiths.find(forge.ownerId);
        sender.sendMessage(color("&6" + forge.name + " &7-- owned by &f" + forge.ownerName
                + (owner != null ? " &7(" + owner.title() + ", level " + owner.level() + ")" : "")));
        sender.sendMessage(color("&7World " + forge.world + " -- " + forge.radius + "-block radius"
                + " -- " + forge.itemsForged + " item(s) forged here."));
    }

    private void handleForgeTip(CommandSender sender, String[] args) {
        if (!(sender instanceof Player payer)) {
            sender.sendMessage(color("&cOnly a player can do that."));
            return;
        }
        if (args.length < 4) {
            payer.sendMessage(color("&6Usage: &f/guild forge tip <forgeName> <amount>"));
            return;
        }
        Forge forge = forges.byName(args[2]);
        if (forge == null) {
            payer.sendMessage(color("&cNo forge by that name."));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            payer.sendMessage(color("&cAmount must be a number."));
            return;
        }
        if (amount <= 0) {
            payer.sendMessage(color("&cAmount must be positive."));
            return;
        }
        if (!economy.isVaultConnected()) {
            payer.sendMessage(color("&cNo economy plugin is connected -- forge tips need Vault and a"
                    + " Vault-compatible economy plugin installed."));
            return;
        }
        if (!economy.withdraw(payer, amount)) {
            payer.sendMessage(color("&cYou don't have " + economy.format(amount) + " to tip."));
            return;
        }
        // Deposited via the OfflinePlayer overload deliberately -- the owner doesn't need to be
        // online right now for their tip to actually land (see EconomyBridge's own comment); the
        // live "you were tipped" message below is just a bonus if they happen to be online.
        economy.deposit(Bukkit.getOfflinePlayer(forge.ownerId), amount);
        Player owner = Bukkit.getPlayer(forge.ownerId);
        if (owner != null) {
            owner.sendMessage(color("&6" + payer.getName() + " &r&6tipped your forge " + forge.name
                    + " &f" + economy.format(amount) + "&6!"));
        }
        SmithProfile ownerProfile = smiths.getOrCreate(forge.ownerId, forge.ownerName);
        smiths.addReputation(ownerProfile, 1);
        payer.sendMessage(color("&aTipped " + forge.name + " " + economy.format(amount) + "."));
    }

    // ---- contracts ----

    private void handleContracts(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            handleContractsList(sender);
            return;
        }
        if (args[1].equalsIgnoreCase("turnin")) {
            handleContractsTurnIn(sender, args);
            return;
        }
        sender.sendMessage(color("&6Usage: &f/guild contracts <list|turnin> [id]"));
    }

    private void handleContractsList(CommandSender sender) {
        List<GuildContractTemplate> all = contracts.all();
        if (all.isEmpty()) {
            sender.sendMessage(color("&7The Guild has no standing contracts posted right now."));
            return;
        }
        sender.sendMessage(color("&6Guild Contracts &7-- /guild contracts turnin <id>"));
        for (GuildContractTemplate contract : all) {
            sender.sendMessage(color("&7- &f" + contract.id() + "&7: " + contract.description()
                    + " &7-- pays &f" + economy.format(contract.goldReward()) + "&7, +" + contract.xpReward()
                    + " xp, +" + contract.reputationReward() + " reputation"));
        }
    }

    private void handleContractsTurnIn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly a player can do that."));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(color("&6Usage: &f/guild contracts turnin <id>"));
            return;
        }
        GuildContractTemplate contract = contracts.byId(args[2]);
        if (contract == null) {
            player.sendMessage(color("&cNo contract by that id -- see &f/guild contracts list"));
            return;
        }
        if (contracts.countHeld(player, contract.material()) < contract.amount()) {
            player.sendMessage(color("&cYou need " + contract.amount() + " "
                    + contract.material().name().toLowerCase() + " to turn this in."));
            return;
        }
        contracts.remove(player, contract.material(), contract.amount());
        if (contract.goldReward() > 0) {
            economy.deposit(player, contract.goldReward());
        }
        SmithProfile profile = smiths.getOrCreate(player.getUniqueId(), player.getName());
        int levelBefore = smiths.addXp(profile, contract.xpReward());
        smiths.addReputation(profile, contract.reputationReward());
        player.sendMessage(color("&aContract fulfilled! &7+" + economy.format(contract.goldReward())
                + " &7, +" + contract.xpReward() + " xp, +" + contract.reputationReward() + " reputation."));
        if (profile.level() > levelBefore) {
            player.sendMessage(color("&6&lThe Crafters Guild recognizes your growing skill -- you are now a level "
                    + profile.level() + " " + profile.title() + "!"));
        }
    }

    // ---- events ----

    private void handleEvents(CommandSender sender) {
        List<WorldEvent> active = events.active();
        if (active.isEmpty()) {
            sender.sendMessage(color("&7No Guild world events are happening right now -- check back soon."));
            return;
        }
        sender.sendMessage(color("&6Active Guild world events:"));
        long now = System.currentTimeMillis();
        for (WorldEvent event : active) {
            long secondsLeft = Math.max(0, (event.expiresAtMillis - now) / 1000);
            sender.sendMessage(color("&7- &f" + eventLabel(event.type) + " &7at &f" + event.x + ", " + event.z
                    + " &7in " + event.world + " &7-- " + secondsLeft + "s remaining"));
        }
    }

    private String eventLabel(EventType type) {
        return switch (type) {
            case RAIDER_INCURSION -> "Raider Incursion";
            case RICH_VEIN -> "Rich Vein";
            case SUPPLY_CARAVAN -> "Supply Caravan";
        };
    }

    // ---- admin ----

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexuscraftersguild.admin")) {
            sender.sendMessage(color("&cYou don't have permission for that."));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(color("&aReloaded Guild Contracts and world-event loot pools. Other tuning"
                + " values (smithing XP rates, forge radii, event timing/rewards) need a full restart"
                + " to take effect -- see README."));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(color("&6NexusCraftersGuild &7-- /guild <profile|forge|contracts|events> &7-- admin: <reload>"));
    }

    private String color(String s) {
        return Colors.color(s);
    }
}
