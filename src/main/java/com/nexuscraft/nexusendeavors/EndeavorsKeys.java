package com.nexuscraft.nexusendeavors;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** PersistentDataContainer keys used across this plugin -- same unspoofable-tag philosophy every
 *  other Nexus plugin uses: a vendor GUI's item is tagged with the exact cosmetic id it represents
 *  the instant {@link CosmeticVendor} builds the menu, so a click is resolved by that unspoofable
 *  tag, never by slot index or display name. */
final class EndeavorsKeys {

    final NamespacedKey vendorItemId;

    EndeavorsKeys(JavaPlugin plugin) {
        this.vendorItemId = new NamespacedKey(plugin, "vendor-item-id");
    }

    static final PersistentDataType<String, String> STRING = PersistentDataType.STRING;
}
