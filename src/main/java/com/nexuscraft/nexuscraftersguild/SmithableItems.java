package com.nexuscraft.nexuscraftersguild;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * The whitelist of Crafter-block outputs this plugin actually treats as "smithing," and what kind
 * of gear each one is -- everything else a Crafter produces (torches, arrows, whatever else a
 * player rigs up) passes straight through completely untouched, exactly like a vanilla Crafter
 * with no plugin installed at all. Deliberately a closed, explicit whitelist rather than "any item
 * with durability" or similar heuristic: this is the one place quality tiers and profession XP can
 * ever apply, so it needs to be obvious and easy to extend by hand, not inferred.
 */
final class SmithableItems {

    enum Category {
        WEAPON,
        ARMOR,
        TOOL
    }

    private static final Map<Material, Category> CATEGORY = new EnumMap<>(Material.class);

    static {
        CATEGORY.put(Material.IRON_SWORD, Category.WEAPON);
        CATEGORY.put(Material.DIAMOND_SWORD, Category.WEAPON);
        CATEGORY.put(Material.NETHERITE_SWORD, Category.WEAPON);
        CATEGORY.put(Material.SHIELD, Category.WEAPON);

        CATEGORY.put(Material.IRON_HELMET, Category.ARMOR);
        CATEGORY.put(Material.IRON_CHESTPLATE, Category.ARMOR);
        CATEGORY.put(Material.IRON_LEGGINGS, Category.ARMOR);
        CATEGORY.put(Material.IRON_BOOTS, Category.ARMOR);
        CATEGORY.put(Material.DIAMOND_HELMET, Category.ARMOR);
        CATEGORY.put(Material.DIAMOND_CHESTPLATE, Category.ARMOR);
        CATEGORY.put(Material.DIAMOND_LEGGINGS, Category.ARMOR);
        CATEGORY.put(Material.DIAMOND_BOOTS, Category.ARMOR);
        CATEGORY.put(Material.NETHERITE_HELMET, Category.ARMOR);
        CATEGORY.put(Material.NETHERITE_CHESTPLATE, Category.ARMOR);
        CATEGORY.put(Material.NETHERITE_LEGGINGS, Category.ARMOR);
        CATEGORY.put(Material.NETHERITE_BOOTS, Category.ARMOR);
        CATEGORY.put(Material.TURTLE_HELMET, Category.ARMOR);

        CATEGORY.put(Material.IRON_PICKAXE, Category.TOOL);
        CATEGORY.put(Material.IRON_AXE, Category.TOOL);
        CATEGORY.put(Material.DIAMOND_PICKAXE, Category.TOOL);
        CATEGORY.put(Material.DIAMOND_AXE, Category.TOOL);
        CATEGORY.put(Material.NETHERITE_PICKAXE, Category.TOOL);
        CATEGORY.put(Material.NETHERITE_AXE, Category.TOOL);
        CATEGORY.put(Material.IRON_HOE, Category.TOOL);
        CATEGORY.put(Material.DIAMOND_HOE, Category.TOOL);
        CATEGORY.put(Material.NETHERITE_HOE, Category.TOOL);
    }

    private SmithableItems() {
    }

    static Category categoryOf(Material material) {
        return CATEGORY.get(material);
    }

    static boolean isSmithable(Material material) {
        return CATEGORY.containsKey(material);
    }
}
