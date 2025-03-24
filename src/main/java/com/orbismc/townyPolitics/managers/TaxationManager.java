package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;

/**
 * Manages taxation modifiers based on corruption levels
 */
public class TaxationManager {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;

    // Values from config.yml
    private static final double DEFAULT_MAX_TAX_PERCENT = 0.0;
    private static final double DEFAULT_MAX_TAX_AMOUNT = 10000.0;

    public TaxationManager(TownyPolitics plugin, CorruptionManager corruptionManager) {
        this.plugin = plugin;
        this.corruptionManager = corruptionManager;
    }

    /**
     * Get the modified maximum tax rate for a town, considering its nation's corruption
     *
     * @param town The town to get max tax for
     * @param isPercentage Whether to get percentage tax or flat tax limit
     * @return The modified maximum tax rate
     */
    public double getModifiedMaxTaxRate(Town town, boolean isPercentage) {
        // Get base max tax - using values from Towny config
        double baseTaxRate;
        if (isPercentage) {
            // Use percentage value if configured, otherwise use default
            baseTaxRate = DEFAULT_MAX_TAX_PERCENT > 0 ?
                    DEFAULT_MAX_TAX_PERCENT : 25.0; // Fallback to 25% if not set
        } else {
            baseTaxRate = DEFAULT_MAX_TAX_AMOUNT;
        }

        // If town doesn't have a nation, return default
        if (!town.hasNation()) {
            return baseTaxRate;
        }

        // Get town's nation
        Nation nation;
        try {
            nation = town.getNation();
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting nation for tax calculation: " + e.getMessage());
            return baseTaxRate;
        }

        // Apply nation's corruption modifier
        double taxModifier = corruptionManager.getTaxationModifier(nation);
        double modifiedMaxTax = baseTaxRate * (1 + taxModifier);

        // Enforce minimum of 0
        return Math.max(0, modifiedMaxTax);
    }

    /**
     * Get the modified maximum tax amount for percentage taxation
     *
     * @param town The town to get max tax for
     * @return The modified maximum tax amount
     */
    public double getModifiedMaxTaxPercentAmount(Town town) {
        double baseMaxAmount = DEFAULT_MAX_TAX_AMOUNT;

        // If town doesn't have a nation, return default
        if (!town.hasNation()) {
            return baseMaxAmount;
        }

        // Get town's nation
        Nation nation;
        try {
            nation = town.getNation();

            // Apply nation's corruption modifier
            double taxModifier = corruptionManager.getTaxationModifier(nation);
            double modifiedMaxAmount = baseMaxAmount * (1 + taxModifier);

            // Enforce minimum of 0
            return Math.max(0, modifiedMaxAmount);
        } catch (Exception e) {
            plugin.getLogger().warning("Error calculating max tax percent amount: " + e.getMessage());
            return baseMaxAmount;
        }
    }

    /**
     * Check if a tax rate is valid for a town considering corruption modifiers
     *
     * @param town The town
     * @param taxRate The tax rate to check
     * @param isPercentage Whether this is a percentage tax rate
     * @return True if the tax rate is valid, false otherwise
     */
    public boolean isValidTaxRate(Town town, double taxRate, boolean isPercentage) {
        return taxRate <= getModifiedMaxTaxRate(town, isPercentage);
    }

    /**
     * Calculate how much the max tax rate has been modified by corruption
     *
     * @param town The town
     * @param isPercentage Whether to calculate for percentage tax
     * @return The taxation modifier (as a multiplier, e.g. 1.05 for +5%)
     */
    public double getTaxModifier(Town town, boolean isPercentage) {
        // If town doesn't have a nation, no modifier
        if (!town.hasNation()) {
            return 1.0;
        }

        // Get town's nation
        Nation nation;
        try {
            nation = town.getNation();
            return 1.0 + corruptionManager.getTaxationModifier(nation);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting tax modifier: " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * Format the tax modification as a percent with sign
     *
     * @param town The town
     * @param isPercentage Whether to format for percentage tax
     * @return The formatted tax modification string
     */
    public String getFormattedTaxModifier(Town town, boolean isPercentage) {
        double modifier = getTaxModifier(town, isPercentage) - 1.0; // -1 to get just the modifier part
        return String.format("%+.1f%%", modifier * 100);
    }
}