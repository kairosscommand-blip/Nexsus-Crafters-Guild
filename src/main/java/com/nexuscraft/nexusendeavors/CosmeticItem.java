package com.nexuscraft.nexusendeavors;

import org.bukkit.Material;

/** One purchasable entry in the vendor -- see {@link CosmeticVendor}. Deliberately cosmetic-only
 *  (dyed gear, banners, hats-flavored blocks, name-flavored books) -- no stat items, so spending
 *  Endeavor Seals is never a power shortcut around actually playing. */
record CosmeticItem(String id, Material material, String displayName, long priceSeals) {
}
