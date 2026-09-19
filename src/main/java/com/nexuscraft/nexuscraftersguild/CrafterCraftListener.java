package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

/**
 * The whole point of this plugin: a real vanilla Auto Crafter, sitting inside a claimed forge's
 * radius, quietly turns every whitelisted item it produces into a piece of that smith's actual
 * work -- tagged, named, and statted -- instead of an anonymous stack of gear.
 *
 * <p><b>Why this rewards automation instead of fighting it.</b> {@code CrafterCraftEvent} (see its
 * own class comment in the stub tree for this session's honest uncertainty about its real shape)
 * fires purely from the block's own redstone trigger -- there is no player in the event at all,
 * whether that trigger was a lever a player just flipped or a hopper-fed clock built a week ago and
 * never touched since. Rather than fight that to try to detect "was a human standing here," this
 * plugin leans into it: ANY output from a Crafter inside your claimed forge counts, by design --
 * that's what makes "mass-production automation chains" a real, first-class part of the profession
 * instead of a loophole around it. What automation can't buy you is level: quality tiers are capped
 * by {@link SmithProfile#level()} (see {@link QualityTier}), which only rises from total crafted
 * volume -- so a bigger hopper clock still just makes more STANDARD-tier gear faster, not better
 * gear. And a per-forge XP-per-minute cap (see below) means even that volume-to-level path has a
 * ceiling a single redstone clock can't blow past.
 */
final class CrafterCraftListener implements Listener {

    private final JavaPlugin plugin;
    private final ForgeRegistry forges;
    private final SmithRegistry smiths;
    private final GuildKeys keys;
    private final EndeavorsBridge endeavors;
    private final Random random = new Random();

    private final long xpPerWeapon;
    private final long xpPerArmor;
    private final long xpPerTool;
    private final long xpCapPerForgePerMinute;
    private final double weaponAttackBonus;
    private final double armorArmorBonus;
    private final double armorToughnessBonus;

    CrafterCraftListener(JavaPlugin plugin, ForgeRegistry forges, SmithRegistry smiths, GuildKeys keys) {
        this.plugin = plugin;
        this.forges = forges;
        this.smiths = smiths;
        this.keys = keys;
        this.endeavors = new EndeavorsBridge(plugin);
        this.xpPerWeapon = Math.max(0, plugin.getConfig().getInt("smithing.xp-per-weapon", 6));
        this.xpPerArmor = Math.max(0, plugin.getConfig().getInt("smithing.xp-per-armor", 8));
        this.xpPerTool = Math.max(0, plugin.getConfig().getInt("smithing.xp-per-tool", 4));
        this.xpCapPerForgePerMinute = Math.max(0, plugin.getConfig().getInt("smithing.xp-cap-per-forge-per-minute", 400));
        this.weaponAttackBonus = plugin.getConfig().getDouble("smithing.weapon-attack-bonus", 1.5);
        this.armorArmorBonus = plugin.getConfig().getDouble("smithing.armor-armor-bonus", 1.0);
        this.armorToughnessBonus = plugin.getConfig().getDouble("smithing.armor-toughness-bonus", 0.5);
    }

    @EventHandler
    public void onCraft(CrafterCraftEvent event) {
        if (event.getBlock() == null || event.getResult() == null) {
            return;
        }
        Location location = event.getBlock().getLocation();
        Forge forge = forges.forgeAt(location);
        if (forge == null) {
            return; // not inside anyone's claimed forge -- leave the vanilla craft completely alone
        }
        ItemStack result = event.getResult();
        SmithableItems.Category category = SmithableItems.categoryOf(result.getType());
        if (category == null) {
            return; // this forge's Crafter is making something this plugin doesn't treat as smithing
        }

        SmithProfile smith = smiths.getOrCreate(forge.ownerId, forge.ownerName);
        QualityTier tier = QualityTier.roll(smith.level(), random);

        applyQuality(result, category, tier, smith, forge);
        event.setResult(result);

        forge.itemsForged++;
        awardXp(forge, smith, category);
        endeavors.report(smith.playerId, smith.playerName, "forge.craft", 1);
    }

    private void applyQuality(ItemStack item, SmithableItems.Category category, QualityTier tier, SmithProfile smith, Forge forge) {
        ItemMeta meta = item.getItemMeta();

        String itemLabel = prettyName(item.getType());
        meta.setDisplayName(tier.colored(tier.displayName + " " + itemLabel));
        meta.setLore(List.of(
                Colors.color("&7Forged by &f" + smith.playerName),
                Colors.color("&7Quality: " + tier.colored(tier.displayName))
        ));

        switch (category) {
            case WEAPON -> addModifier(meta, Attribute.ATTACK_DAMAGE, "attack",
                    weaponAttackBonus * tier.statMultiplier);
            case ARMOR -> {
                addModifier(meta, Attribute.ARMOR, "armor", armorArmorBonus * tier.statMultiplier);
                addModifier(meta, Attribute.ARMOR_TOUGHNESS, "toughness", armorToughnessBonus * tier.statMultiplier);
            }
            case TOOL -> {
                // No matching vanilla Attribute for mining speed/efficiency in this stub's surface
                // (see README) -- a tool's quality tier is cosmetic-only for now: the name, lore,
                // and the smith's own bragging rights, same as a plain STANDARD tool with better
                // flavor text. A future version adding a real efficiency hook is a natural next step.
            }
        }

        meta.getPersistentDataContainer().set(keys.forgedByForgeId, GuildKeys.STRING, forge.id.toString());
        meta.getPersistentDataContainer().set(keys.forgedBySmithId, GuildKeys.STRING, smith.playerId.toString());
        meta.getPersistentDataContainer().set(keys.forgedBySmithName, GuildKeys.STRING, smith.playerName);
        meta.getPersistentDataContainer().set(keys.qualityTier, GuildKeys.STRING, tier.name());
        item.setItemMeta(meta);
    }

    private void addModifier(ItemMeta meta, Attribute attribute, String slug, double amount) {
        if (amount <= 0) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "guild-" + slug + "-bonus");
        meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void awardXp(Forge forge, SmithProfile smith, SmithableItems.Category category) {
        long now = System.currentTimeMillis();
        if (now - forge.xpWindowStartMillis > 60_000L) {
            forge.xpWindowStartMillis = now;
            forge.xpThisWindow = 0;
        }
        if (forge.xpThisWindow >= xpCapPerForgePerMinute) {
            return; // this forge has already fed its owner the max XP this plugin allows for one minute
        }

        long baseXp = switch (category) {
            case WEAPON -> xpPerWeapon;
            case ARMOR -> xpPerArmor;
            case TOOL -> xpPerTool;
        };
        long grant = Math.min(baseXp, xpCapPerForgePerMinute - forge.xpThisWindow);
        if (grant <= 0) {
            return;
        }
        forge.xpThisWindow += grant;

        int levelBefore = smiths.addXp(smith, grant);
        if (smith.level() > levelBefore) {
            Player owner = Bukkit.getPlayer(smith.playerId);
            if (owner != null) {
                owner.sendMessage(Colors.color("&6&lThe Crafters Guild &r&6recognizes your growing skill -- "
                        + "you are now a level " + smith.level() + " " + smith.title() + "!"));
            }
        }
    }

    private static String prettyName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
