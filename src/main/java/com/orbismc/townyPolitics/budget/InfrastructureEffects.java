package com.orbismc.townyPolitics.budget;

/**
 * Class to encapsulate all infrastructure budget effects
 */
public class InfrastructureEffects {
    private final double claimCostModifier;
    private final double townBlockBonusModifier;
    private final double wallPlotsModifier; // Placeholder for future wall plots feature

    public InfrastructureEffects(double claimCostModifier, double townBlockBonusModifier, double wallPlotsModifier) {
        this.claimCostModifier = claimCostModifier;
        this.townBlockBonusModifier = townBlockBonusModifier;
        this.wallPlotsModifier = wallPlotsModifier;
    }

    public InfrastructureEffects(double claimCostModifier, double townBlockBonusModifier) {
        this(claimCostModifier, townBlockBonusModifier, 1.0); // Default wall plots modifier
    }

    public double getClaimCostModifier() {
        return claimCostModifier;
    }

    public double getTownBlockBonusModifier() {
        return townBlockBonusModifier;
    }

    public double getWallPlotsModifier() {
        return wallPlotsModifier;
    }
}