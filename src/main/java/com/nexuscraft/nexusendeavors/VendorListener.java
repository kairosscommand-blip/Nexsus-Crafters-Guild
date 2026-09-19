package com.nexuscraft.nexusendeavors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Handles a click inside the vendor GUI (see {@link CosmeticVendor#open}) -- every click here is
 *  cancelled outright (nothing can be dragged out of the shop's own display slots), and a click on
 *  an actual priced item attempts the purchase. */
final class VendorListener implements Listener {

    private final CosmeticVendor vendor;
    private final EndeavorProgressEngine engine;
    private final EndeavorsKeys keys;

    VendorListener(CosmeticVendor vendor, EndeavorProgressEngine engine, EndeavorsKeys keys) {
        this.vendor = vendor;
        this.engine = engine;
        this.keys = keys;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !(clicked.getHolder() instanceof VendorInventoryHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta()) {
            return;
        }
        String itemId = current.getItemMeta().getPersistentDataContainer().get(keys.vendorItemId, EndeavorsKeys.STRING);
        CosmeticItem item = vendor.byId(itemId);
        if (item == null) {
            return;
        }
        if (!engine.spend(player.getUniqueId(), item.priceSeals())) {
            player.sendMessage(Colors.color("&cYou don't have " + item.priceSeals() + " Endeavor Seals for that."));
            return;
        }
        var leftover = player.getInventory().addItem(vendor.buildPurchasedItem(item));
        if (!leftover.isEmpty()) {
            // Couldn't actually fit -- refund rather than let a bought item vanish into a full
            // inventory (real Bukkit's addItem hands back exactly what it couldn't place).
            engine.refund(player.getUniqueId(), item.priceSeals());
            player.sendMessage(Colors.color("&cYour inventory is full -- refunded " + item.priceSeals() + " Endeavor Seals."));
            return;
        }
        player.sendMessage(Colors.color("&aPurchased " + item.displayName() + " &7for " + item.priceSeals() + " Endeavor Seals."));
    }
}
