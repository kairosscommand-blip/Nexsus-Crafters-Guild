package com.nexuscraft.nexusendeavors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class EndeavorsCommandExecutor implements CommandExecutor {

    private final NexusEndeavors plugin;
    private final EndeavorProgressEngine engine;
    private final CosmeticVendor vendor;

    EndeavorsCommandExecutor(NexusEndeavors plugin, EndeavorProgressEngine engine, CosmeticVendor vendor) {
        this.plugin = plugin;
        this.engine = engine;
        this.vendor = vendor;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            handleBoard(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "board" -> handleBoard(sender);
            case "streak" -> handleStreak(sender);
            case "vendor" -> handleVendor(sender);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleBoard(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly a player can do that."));
            return;
        }
        PlayerEndeavorProgress profile = engine.registry().getOrCreate(player.getUniqueId(), player.getName());
        engine.ensureCurrent(profile);

        player.sendMessage(color("&6&lThe Crafters Guild's board &r&6-- " + profile.seals + " Endeavor Seals"));
        player.sendMessage(color("&7Daily:"));
        for (ObjectiveDefinition def : engine.activeDaily()) {
            sendObjectiveLine(player, def, profile.dailyProgress, profile.dailyCompleted);
        }
        player.sendMessage(color("&7Weekly:"));
        for (ObjectiveDefinition def : engine.activeWeekly()) {
            sendObjectiveLine(player, def, profile.weeklyProgress, profile.weeklyCompleted);
        }
    }

    private void sendObjectiveLine(Player player, ObjectiveDefinition def,
            java.util.Map<String, Integer> progressMap, java.util.Set<String> completedSet) {
        int progress = progressMap.getOrDefault(def.id(), 0);
        boolean done = completedSet.contains(def.id());
        player.sendMessage(color((done ? "&a  ✓ " : "&7  - ") + "&f" + def.description()
                + " &7(" + progress + "/" + def.target() + ") &7-- &f" + def.rewardSeals() + " seals"
                + (done ? " &a[complete]" : "")));
    }

    private void handleStreak(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly a player can do that."));
            return;
        }
        PlayerEndeavorProgress profile = engine.registry().find(player.getUniqueId());
        int streak = profile != null ? profile.loginStreak : 0;
        player.sendMessage(color("&6Your login streak: &f" + streak + " day(s) in a row."));
    }

    private void handleVendor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly a player can do that."));
            return;
        }
        vendor.open(player);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexusendeavors.admin")) {
            sender.sendMessage(color("&cYou don't have permission for that."));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(color("&aReloaded the objective pools, vendor, active-count/reset-offset settings,"
                + " and streak milestones. The board's rotation is still deterministic from the pool + today's"
                + " date, so today's already-active objective set only changes if the reload adds/removes pool"
                + " entries that shuffle the pick -- see README."));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(color("&6NexusEndeavors &7-- /endeavors <board|streak|vendor> &7-- admin: <reload>"));
    }

    private String color(String s) {
        return Colors.color(s);
    }
}
