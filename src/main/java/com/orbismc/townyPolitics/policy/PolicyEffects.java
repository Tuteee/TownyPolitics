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

    public PolicyEffects(double taxModifier, double tradeModifier, double economyModifier,
                         double politicalPowerGainModifier, double corruptionGainModifier, double maxPoliticalPowerModifier,
                         double resourceOutputModifier, double spendingModifier) {
        this.taxModifier = taxModifier;
        this.tradeModifier = tradeModifier;
        this.economyModifier = economyModifier;
        this.politicalPowerGainModifier = politicalPowerGainModifier;
        this.corruptionGainModifier = corruptionGainModifier;
        this.maxPoliticalPowerModifier = maxPoliticalPowerModifier;
        this.resourceOutputModifier = resourceOutputModifier;
        this.spendingModifier = spendingModifier;
    }

    // Getters
    public double getTaxModifier() { return taxModifier; }
    public double getTradeModifier() { return tradeModifier; }
    public double getEconomyModifier() { return economyModifier; }
    public double getPoliticalPowerGainModifier() { return politicalPowerGainModifier; }
    public double getCorruptionGainModifier() { return corruptionGainModifier; }
    public double getMaxPoliticalPowerModifier() { return maxPoliticalPowerModifier; }
    public double getResourceOutputModifier() { return resourceOutputModifier; }
    public double getSpendingModifier() { return spendingModifier; }

    /**
     * Builder class for PolicyEffects
     */
    public static class Builder {
        private double taxModifier = 1.0;
        private double tradeModifier = 1.0;
        private double economyModifier = 1.0;
        private double politicalPowerGainModifier = 1.0;
        private double corruptionGainModifier = 1.0;
        private double maxPoliticalPowerModifier = 1.0;
        private double resourceOutputModifier = 1.0;
        private double spendingModifier = 1.0;

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

        public PolicyEffects build() {
            return new PolicyEffects(
                    taxModifier, tradeModifier, economyModifier,
                    politicalPowerGainModifier, corruptionGainModifier, maxPoliticalPowerModifier,
                    resourceOutputModifier, spendingModifier
            );
        }
    }
}