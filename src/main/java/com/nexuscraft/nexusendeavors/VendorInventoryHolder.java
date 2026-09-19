package com.nexuscraft.nexusendeavors;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marks an open inventory as this plugin's cosmetics vendor GUI, so {@link VendorListener} can
 *  tell a click here apart from a click in any other inventory the player might have open (their
 *  own, a chest, another plugin's menu) purely by holder identity -- never by title text, which is
 *  cosmetic and could coincidentally collide. */
final class VendorInventoryHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
