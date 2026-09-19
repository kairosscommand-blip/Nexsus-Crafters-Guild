package com.nexuscraft.nexuscraftersguild;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Everything this plugin needs from the economy side of the server, in one place -- the same
 * Vault-soft-integration pattern NexusCreativeSurvival's own EconomyBridge uses (copied and
 * trimmed: this plugin has no NexusEconomy-specific pricing to bridge, just gold moving between
 * players and the guild -- forge tips, commission payments, and Guild Contract/world-event
 * payouts). Works with any Vault-compatible economy plugin, not tied to NexusEconomy specifically;
 * every method is a safe, silent no-op/false/zero when no economy plugin is installed at all, so
 * the guild's non-money systems (levels, quality tiers, reputation, contracts, events) all still
 * work on a server with no economy plugin -- gold rewards just don't pay out.
 */
final class EconomyBridge {

    private Economy vaultEconomy;

    boolean isVaultConnected() {
        if (vaultEconomy == null) {
            tryConnect();
        }
        return vaultEconomy != null;
    }

    private void tryConnect() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            vaultEconomy = provider.getProvider();
        }
    }

    boolean has(Player player, double amount) {
        return isVaultConnected() && vaultEconomy.has(player, amount);
    }

    /** Withdraws the amount, returning whether it actually succeeded (mirrors Vault's own
     *  EconomyResponse.transactionSuccess). */
    boolean withdraw(Player player, double amount) {
        if (!isVaultConnected()) {
            return false;
        }
        return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** Deposits the amount -- used for forge tips reaching the smith, and Guild Contract/world
     *  event gold rewards. Silently does nothing if no Vault economy is connected; callers don't
     *  need to check isVaultConnected() first for a deposit, only when they need to KNOW it landed
     *  (a tip, where the payer should be told it failed). */
    boolean deposit(Player player, double amount) {
        if (!isVaultConnected() || amount <= 0) {
            return false;
        }
        return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
    }

    /** Same as {@link #deposit(Player, double)} but works for a currently-offline owner too (a
     *  forge tip must never be withdrawn from the payer and then have nowhere to land just because
     *  the forge's owner happens to be offline right now -- Vault's own Economy interface pays an
     *  OfflinePlayer's balance directly, no online session required). */
    boolean deposit(OfflinePlayer player, double amount) {
        if (!isVaultConnected() || amount <= 0) {
            return false;
        }
        return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
    }

    String format(double amount) {
        if (isVaultConnected()) {
            return vaultEconomy.format(amount);
        }
        return String.format("%,.2f gold", amount);
    }
}
