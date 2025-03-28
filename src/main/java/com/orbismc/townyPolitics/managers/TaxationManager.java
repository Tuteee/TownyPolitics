package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.utils.DelegateLogger;

/**
 * Manages taxation modifiers based on corruption levels and government types
 */
public class TaxationManager {

    private final TownyPolitics plugin;
    private final CorruptionManager nationCorruptionManager;
    private final DelegateLogger logger;

    public TaxationManager(TownyPolitics plugin, CorruptionManager nationCorruptionManager) {
        this.plugin = plugin;
        this.nationCorruptionManager = nationCorruptionManager;
        this.logger = new DelegateLogger(plugin, "TaxManager");
        logger.info("Taxation Manager initialized");
    }

    /**
     * Get the modified maximum tax rate for a town, considering its nation's corruption
     *
     * @param town The town to get max tax for
     * @param isPercentage Whether to get percentage tax or flat tax limit
     * @return The modified maximum tax rate
     */
    public double getModifiedMaxTaxRate(Town town, boolean isPercentage) {
        // Get base max tax from Towny config
        double baseTaxRate;
        if (isPercentage) {
            // Read max_town_tax_percent from config
            baseTaxRate = Double.parseDouble(plugin.getConfig().getString("daily_taxes.max_town_tax_percent", "2"));

            // If the value is 0, use a default percentage
            if (baseTaxRate <= 0) {
                baseTaxRate = 25.0; // Fallback to 25% if not set or set to 0
            }
        } else {
            // Read max_town_tax_amount from config
            baseTaxRate = Double.parseDouble(plugin.getConfig().getString("daily_taxes.max_town_tax_amount", "0"));

            // If the value is 0, use a reasonable default
            if (baseTaxRate <= 0) {
                baseTaxRate = 1000.0; // Default flat tax amount
            }
        }

        // If town doesn't have a nation, return default
        if (!town.hasNation()) {
            logger.fine("Town " + town.getName() + " has no nation, using base tax rate: " + baseTaxRate);
            return baseTaxRate;
        }

        // Get town's nation
        Nation nation;
        try {
            nation = town.getNation();
        } catch (Exception e) {
            logger.warning("Error getting nation for tax calculation: " + e.getMessage());
            return baseTaxRate;
        }

        // Apply nation's corruption modifier
        double taxModifier = nationCorruptionManager.getTaxationModifier(nation);
        double modifiedMaxTax = baseTaxRate * taxModifier;

        logger.fine("Town " + town.getName() + " in nation " + nation.getName() +
                " has modified max tax rate: " + modifiedMaxTax +
                " (base: " + baseTaxRate + ", modifier: " + taxModifier + ")");

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
        // Read max_town_tax_percent_amount from config
        double baseMaxAmount = Double.parseDouble(plugin.getConfig().getString("daily_taxes.max_town_tax_percent_amount", "100"));

        // If the value is 0, use a reasonable default
        if (baseMaxAmount <= 0) {
            baseMaxAmount = 1000.0; // Default max amount
        }

        // If town doesn't have a nation, return default
        if (!town.hasNation()) {
            logger.fine("Town " + town.getName() + " has no nation, using base max tax amount: " + baseMaxAmount);
            return baseMaxAmount;
        }

        // Get town's nation
        Nation nation;
        try {
            nation = town.getNation();

            // Apply nation's corruption modifier
            double taxModifier = nationCorruptionManager.getTaxationModifier(nation);
            double modifiedMaxAmount = baseMaxAmount * taxModifier;

            logger.fine("Town " + town.getName() + " in nation " + nation.getName() +
                    " has modified max tax amount: " + modifiedMaxAmount +
                    " (base: " + baseMaxAmount + ", modifier: " + taxModifier + ")");

            // Enforce minimum of 0
            return Math.max(0, modifiedMaxAmount);
        } catch (Exception e) {
            logger.warning("Error calculating max tax percent amount: " + e.getMessage());
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
        double modifiedMaxRate = getModifiedMaxTaxRate(town, isPercentage);
        boolean isValid = taxRate <= modifiedMaxRate;

        logger.fine("Checking if tax rate " + taxRate + " is valid for town " + town.getName() +
                ": " + isValid + " (max: " + modifiedMaxRate + ")");

        return isValid;
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
            double modifier = nationCorruptionManager.getTaxationModifier(nation);

            logger.fine("Tax modifier for town " + town.getName() + " in nation " +
                    nation.getName() + ": " + modifier);

            return modifier;
        } catch (Exception e) {
            logger.warning("Error getting tax modifier: " + e.getMessage());
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

    /**
     * Get the tax penalty modifier based on corruption level
     * Higher corruption = higher percentage of taxes lost to corruption
     *
     * @param nation The nation
     * @return The tax penalty modifier (0.0 to 1.0)
     */
    public double getTaxPenaltyModifier(Nation nation) {
        // Get the corruption level
        double corruption = nationCorruptionManager.getCorruption(nation);

        // Calculate penalty based on corruption threshold level
        int thresholdLevel = nationCorruptionManager.getCorruptionThresholdLevel(nation);

        // Calculate penalty modifier based on threshold level
        double penaltyModifier = switch (thresholdLevel) {
            case 0 -> 0.0;                  // No tax penalty
            case 1 -> 0.05;                 // 5% of taxes lost to corruption
            case 2 -> 0.15;                 // 15% of taxes lost to corruption
            case 3 -> 0.25;                 // 25% of taxes lost to corruption
            case 4 -> 0.5;                  // 50% of taxes lost to corruption (critical level)
            default -> 0.0;
        };

        logger.fine("Nation " + nation.getName() + " has tax penalty modifier: " +
                penaltyModifier + " (corruption level: " + thresholdLevel + ")");

        return penaltyModifier;
    }

    /**
     * Get income generation bonus based on town government type
     *
     * @param town The town to calculate bonus for
     * @return The tax income bonus multiplier (e.g. 1.05 for +5% bonus)
     */
    public double getTaxIncomeBonus(Town town) {
        // Default is no bonus
        double bonus = 1.0;

        try {
            // Get town government type
            GovernmentType govType = plugin.getTownGovManager().getGovernmentType(town);

            // Apply appropriate government bonus
            switch (govType) {
                case AUTOCRACY -> {
                    bonus = 1.05; // +5% Tax income from Effective Control
                    logger.fine("Town " + town.getName() + " gets +5% tax income from Autocracy government");
                }
                case REPUBLIC -> {
                    bonus = 1.02; // +2% Tax income from Economic Boom
                    logger.fine("Town " + town.getName() + " gets +2% tax income from Republic government");
                }
                case DIRECT_DEMOCRACY -> {
                    bonus = 1.02; // +2% Tax income from Economic Prosperity
                    logger.fine("Town " + town.getName() + " gets +2% tax income from Direct Democracy government");
                }
                default -> {
                    // No bonus for other government types
                }
            }

            // Apply town corruption penalty if applicable
            TownCorruptionManager townCorruptionManager = plugin.getTownCorruptionManager();
            double corruptionModifier = townCorruptionManager.getTaxationModifier(town);

            // Final bonus is government bonus multiplied by corruption penalty
            double finalBonus = bonus * corruptionModifier;

            logger.fine("Town " + town.getName() + " final tax income modifier: " + finalBonus +
                    " (gov bonus: " + bonus + ", corruption mod: " + corruptionModifier + ")");

            return finalBonus;
        } catch (Exception e) {
            logger.warning("Error calculating tax income bonus: " + e.getMessage());
            return 1.0; // Default to no bonus on error
        }
    }

    /**
     * Get trade income bonus based on town government type
     *
     * @param town The town to check
     * @return The trade income bonus multiplier (e.g. 1.08 for +8% bonus)
     */
    public double getTradeIncomeBonus(Town town) {
        // Default is no bonus
        double bonus = 1.0;

        try {
            // Get town government type
            GovernmentType govType = plugin.getTownGovManager().getGovernmentType(town);

            // Apply appropriate government bonus
            switch (govType) {
                case OLIGARCHY -> {
                    bonus = 1.08; // +8% Trade income from Elite Connections
                    logger.fine("Town " + town.getName() + " gets +8% trade income from Oligarchy government");
                }
                case REPUBLIC -> {
                    bonus = 1.05; // +5% Trade income from Economic Boom
                    logger.fine("Town " + town.getName() + " gets +5% trade income from Republic government");
                }
                default -> {
                    // No bonus for other government types
                }
            }

            // Apply town corruption penalty if applicable
            TownCorruptionManager townCorruptionManager = plugin.getTownCorruptionManager();
            double corruptionModifier = townCorruptionManager.getTradeModifier(town);

            // Final bonus is government bonus multiplied by corruption penalty
            double finalBonus = bonus * corruptionModifier;

            logger.fine("Town " + town.getName() + " final trade income modifier: " + finalBonus +
                    " (gov bonus: " + bonus + ", corruption mod: " + corruptionModifier + ")");

            return finalBonus;
        } catch (Exception e) {
            logger.warning("Error calculating trade income bonus: " + e.getMessage());
            return 1.0; // Default to no bonus on error
        }
    }
}