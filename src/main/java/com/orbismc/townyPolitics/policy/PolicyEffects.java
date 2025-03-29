package com.orbismc.townyPolitics.policy;

/**
 * Represents the effects a policy can have on gameplay mechanics
 */
public class PolicyEffects {
    // Economic effects
    private final double taxModifier;
    private final double tradeModifier;
    private final double economyModifier;

    // Political effects
    private final double politicalPowerGainModifier;
    private final double corruptionGainModifier;
    private final double maxPoliticalPowerModifier;

    // Resource effects
    private final double resourceOutputModifier;
    private final double spendingModifier;

    // Town-specific effects
    private final double plotCostModifier;
    private final double plotTaxModifier;
    private final double residentCapacityModifier;
    private final double upkeepModifier;
    private final double townBlockCostModifier;
    private final double townBlockBonusModifier;

    public PolicyEffects(double taxModifier, double tradeModifier, double economyModifier,
                         double politicalPowerGainModifier, double corruptionGainModifier, double maxPoliticalPowerModifier,
                         double resourceOutputModifier, double spendingModifier,
                         double plotCostModifier, double plotTaxModifier, double residentCapacityModifier,
                         double upkeepModifier, double townBlockCostModifier, double townBlockBonusModifier) {
        this.taxModifier = taxModifier;
        this.tradeModifier = tradeModifier;
        this.economyModifier = economyModifier;
        this.politicalPowerGainModifier = politicalPowerGainModifier;
        this.corruptionGainModifier = corruptionGainModifier;
        this.maxPoliticalPowerModifier = maxPoliticalPowerModifier;
        this.resourceOutputModifier = resourceOutputModifier;
        this.spendingModifier = spendingModifier;
        this.plotCostModifier = plotCostModifier;
        this.plotTaxModifier = plotTaxModifier;
        this.residentCapacityModifier = residentCapacityModifier;
        this.upkeepModifier = upkeepModifier;
        this.townBlockCostModifier = townBlockCostModifier;
        this.townBlockBonusModifier = townBlockBonusModifier;
    }

    // Getters for existing effects
    public double getTaxModifier() { return taxModifier; }
    public double getTradeModifier() { return tradeModifier; }
    public double getEconomyModifier() { return economyModifier; }
    public double getPoliticalPowerGainModifier() { return politicalPowerGainModifier; }
    public double getCorruptionGainModifier() { return corruptionGainModifier; }
    public double getMaxPoliticalPowerModifier() { return maxPoliticalPowerModifier; }
    public double getResourceOutputModifier() { return resourceOutputModifier; }
    public double getSpendingModifier() { return spendingModifier; }

    // Getters for town-specific effects
    public double getPlotCostModifier() { return plotCostModifier; }
    public double getPlotTaxModifier() { return plotTaxModifier; }
    public double getResidentCapacityModifier() { return residentCapacityModifier; }
    public double getUpkeepModifier() { return upkeepModifier; }
    public double getTownBlockCostModifier() { return townBlockCostModifier; }
    public double getTownBlockBonusModifier() { return townBlockBonusModifier; }

    // Check if town-specific modifiers are present
    public boolean hasTownEffects() {
        return plotCostModifier != 1.0 ||
                plotTaxModifier != 1.0 ||
                residentCapacityModifier != 1.0 ||
                upkeepModifier != 1.0 ||
                townBlockCostModifier != 1.0 ||
                townBlockBonusModifier != 1.0;
    }

    /**
     * Builder class for PolicyEffects
     */
    public static class Builder {
        // Default values (1.0 = no effect)
        private double taxModifier = 1.0;
        private double tradeModifier = 1.0;
        private double economyModifier = 1.0;
        private double politicalPowerGainModifier = 1.0;
        private double corruptionGainModifier = 1.0;
        private double maxPoliticalPowerModifier = 1.0;
        private double resourceOutputModifier = 1.0;
        private double spendingModifier = 1.0;
        private double plotCostModifier = 1.0;
        private double plotTaxModifier = 1.0;
        private double residentCapacityModifier = 1.0;
        private double upkeepModifier = 1.0;
        private double townBlockCostModifier = 1.0;
        private double townBlockBonusModifier = 1.0;

        // nation setters
        public Builder taxModifier(double taxModifier) {
            this.taxModifier = taxModifier;
            return this;
        }

        public Builder tradeModifier(double tradeModifier) {
            this.tradeModifier = tradeModifier;
            return this;
        }

        public Builder economyModifier(double economyModifier) {
            this.economyModifier = economyModifier;
            return this;
        }

        public Builder politicalPowerGainModifier(double politicalPowerGainModifier) {
            this.politicalPowerGainModifier = politicalPowerGainModifier;
            return this;
        }

        public Builder corruptionGainModifier(double corruptionGainModifier) {
            this.corruptionGainModifier = corruptionGainModifier;
            return this;
        }

        public Builder maxPoliticalPowerModifier(double maxPoliticalPowerModifier) {
            this.maxPoliticalPowerModifier = maxPoliticalPowerModifier;
            return this;
        }

        public Builder resourceOutputModifier(double resourceOutputModifier) {
            this.resourceOutputModifier = resourceOutputModifier;
            return this;
        }

        public Builder spendingModifier(double spendingModifier) {
            this.spendingModifier = spendingModifier;
            return this;
        }

        // Town-specific setters
        public Builder plotCostModifier(double plotCostModifier) {
            this.plotCostModifier = plotCostModifier;
            return this;
        }

        public Builder plotTaxModifier(double plotTaxModifier) {
            this.plotTaxModifier = plotTaxModifier;
            return this;
        }

        public Builder residentCapacityModifier(double residentCapacityModifier) {
            this.residentCapacityModifier = residentCapacityModifier;
            return this;
        }

        public Builder upkeepModifier(double upkeepModifier) {
            this.upkeepModifier = upkeepModifier;
            return this;
        }

        public Builder townBlockCostModifier(double townBlockCostModifier) {
            this.townBlockCostModifier = townBlockCostModifier;
            return this;
        }

        public Builder townBlockBonusModifier(double townBlockBonusModifier) {
            this.townBlockBonusModifier = townBlockBonusModifier;
            return this;
        }

        public PolicyEffects build() {
            return new PolicyEffects(
                    taxModifier, tradeModifier, economyModifier,
                    politicalPowerGainModifier, corruptionGainModifier, maxPoliticalPowerModifier,
                    resourceOutputModifier, spendingModifier,
                    plotCostModifier, plotTaxModifier, residentCapacityModifier,
                    upkeepModifier, townBlockCostModifier, townBlockBonusModifier
            );
        }
    }
}