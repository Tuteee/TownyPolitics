package com.orbismc.townyPolitics.budget;

/**
 * Class to encapsulate all education budget effects
 */
public class EducationEffects {
    private final double ppGainModifier;
    private final double policyCostModifier;
    private final double technologyModifier; // Placeholder for future technology features

    public EducationEffects(double ppGainModifier, double policyCostModifier, double technologyModifier) {
        this.ppGainModifier = ppGainModifier;
        this.policyCostModifier = policyCostModifier;
        this.technologyModifier = technologyModifier;
    }

    public EducationEffects(double ppGainModifier, double policyCostModifier) {
        this(ppGainModifier, policyCostModifier, 1.0); // Default technology modifier
    }

    public double getPpGainModifier() {
        return ppGainModifier;
    }

    public double getPolicyCostModifier() {
        return policyCostModifier;
    }

    public double getTechnologyModifier() {
        return technologyModifier;
    }
}