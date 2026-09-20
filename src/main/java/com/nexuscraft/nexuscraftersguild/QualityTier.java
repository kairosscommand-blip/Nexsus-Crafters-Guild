package com.nexuscraft.nexuscraftersguild;

import java.util.Random;

/**
 * Five hand-feel tiers for crafted gear, from a botched job to something worth naming. Each tier
 * has a stat multiplier (applied to the base weapon/armor Attribute bonus -- see
 * CrafterCraftListener) and a minimum smith level before it can ever be rolled at all, so a level 1
 * smith's forge -- hand-run or hopper-automated, this plugin can't tell the difference, see
 * SmithProfile's own comment -- physically cannot produce Legendary gear no matter how much volume
 * runs through it. Climbing the tiers is climbing levels, not stacking Crafters.
 */
enum QualityTier {
    CRUDE("&7", "Crude", 0.80, 1),
    STANDARD("&f", "Standard", 1.0, 1),
    FINE("&a", "Fine", 1.25, 5),
    MASTERWORK("&b", "Masterwork", 1.55, 15),
    LEGENDARY("&6&l", "Legendary", 2.0, 30);

    final String colorCode;
    final String displayName;
    final double statMultiplier;
    final int minLevel;

    QualityTier(String colorCode, String displayName, double statMultiplier, int minLevel) {
        this.colorCode = colorCode;
        this.displayName = displayName;
        this.statMultiplier = statMultiplier;
        this.minLevel = minLevel;
    }

    /**
     * Weighted roll among every tier this level has unlocked. Weight favors CRUDE/STANDARD heavily
     * at low level (a fresh smith mostly turns out ordinary work with the occasional dud) and
     * shifts toward the top tiers as level climbs past each tier's own minLevel -- so hitting a
     * tier's level requirement unlocks it at a low chance first, not a guarantee.
     */
    static QualityTier roll(int smithLevel, Random random) {
        double totalWeight = 0;
        double[] weights = new double[values().length];
        QualityTier[] tiers = values();
        for (int i = 0; i < tiers.length; i++) {
            QualityTier tier = tiers[i];
            if (smithLevel < tier.minLevel) {
                weights[i] = 0;
                continue;
            }
            int levelsPastUnlock = smithLevel - tier.minLevel;
            double weight = switch (tier) {
                case CRUDE -> Math.max(1.0, 12.0 - smithLevel * 0.5);
                case STANDARD -> 10.0;
                case FINE -> 2.0 + levelsPastUnlock * 0.6;
                case MASTERWORK -> 1.0 + levelsPastUnlock * 0.5;
                case LEGENDARY -> 0.5 + levelsPastUnlock * 0.35;
            };
            weights[i] = weight;
            totalWeight += weight;
        }
        double roll = random.nextDouble() * totalWeight;
        double cursor = 0;
        for (int i = 0; i < tiers.length; i++) {
            cursor += weights[i];
            if (roll <= cursor) {
                return tiers[i];
            }
        }
        return STANDARD;
    }

    String colored(String text) {
        return Colors.color(colorCode + text);
    }
}
