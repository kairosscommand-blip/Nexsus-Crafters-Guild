package com.nexuscraft.nexusendeavors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The spend side of the loop -- a plain in-game GUI shop, cosmetic-only rewards, loaded from
 * {@code vendor.items} in config.yml. Deliberately no power items: dyed gear, banners, and
 * hat-flavored blocks (see NexusHats for the real cosmetic-hat mechanic this vendor's hat items are
 * meant to be worn with) keep Endeavor Seals purely a "look" currency, never a shortcut around
 * actually playing.
 */
final class CosmeticVendor {

    private final JavaPlugin plugin;
    private final EndeavorsKeys keys;
    private final Map<String, CosmeticItem> items = new LinkedHashMap<>();

    CosmeticVendor(JavaPlugin plugin, EndeavorsKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    void load() {
        items.clear();
        for (Map<?, ?> raw : plugin.getConfig().getMapList("vendor.items")) {
            CosmeticItem item = parse(raw);
            if (item != null) {
                items.put(item.id(), item);
            }
        }
    }

    private CosmeticItem parse(Map<?, ?> raw) {
        Object idObj = raw.get("id");
        Object materialObj = raw.get("material");
        Material material = materialObj != null ? Material.matchMaterial(String.valueOf(materialObj)) : null;
        if (idObj == null || material == null) {
            plugin.getLogger().warning("[NexusEndeavors] Skipping a malformed vendor item (bad/missing id or material): " + raw);
            return null;
        }
        String displayName = raw.get("name") != null ? String.valueOf(raw.get("name")) : String.valueOf(idObj);
        long price = 0;
        Object priceObj = raw.get("price-seals");
        if (priceObj != null) {
            try {
                price = Long.parseLong(String.valueOf(priceObj));
            } catch (NumberFormatException ignored) {
                // falls through to the price<=0 check below
            }
        }
        if (price <= 0) {
            plugin.getLogger().warning("[NexusEndeavors] Skipping a malformed vendor item '" + idObj + "' (bad price-seals): " + raw);
            return null;
        }
        return new CosmeticItem(String.valueOf(idObj), material, displayName, price);
    }

    CosmeticItem byId(String id) {
        return id == null ? null : items.get(id);
    }

    List<CosmeticItem> all() {
        return new ArrayList<>(items.values());
    }

    void open(Player player) {
        int size = Math.max(9, Math.min(54, ((items.size() + 8) / 9) * 9));
        VendorInventoryHolder holder = new VendorInventoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, Colors.color("&6The Crafters Guild Vendor"));
        holder.setInventory(inventory);

        int slot = 0;
        for (CosmeticItem item : items.values()) {
            if (slot >= size) {
                break; // more items configured than the GUI has room for -- see README
            }
            inventory.setItem(slot++, buildShopIcon(item));
        }
        player.openInventory(inventory);
    }

    private ItemStack buildShopIcon(CosmeticItem item) {
        ItemStack stack = new ItemStack(item.material(), 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Colors.color("&6" + item.displayName()));
        meta.setLore(List.of(Colors.color("&7Price: &f" + item.priceSeals() + " Endeavor Seals"),
                Colors.color("&7Click to purchase.")));
        meta.getPersistentDataContainer().set(keys.vendorItemId, EndeavorsKeys.STRING, item.id());
        stack.setItemMeta(meta);
        return stack;
    }

    /** A clean copy for the player's inventory once bought -- deliberately without the shop's own
     *  PDC/lore/price tagging, so a purchased cosmetic looks like a normal item, not a menu entry. */
    ItemStack buildPurchasedItem(CosmeticItem item) {
        ItemStack stack = new ItemStack(item.material(), 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Colors.color("&6" + item.displayName()));
        stack.setItemMeta(meta);
        return stack;
    }
}
