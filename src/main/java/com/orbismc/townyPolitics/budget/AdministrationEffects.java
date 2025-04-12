package com.orbismc.townyPolitics.budget;

/**
 * Class to encapsulate all administration budget effects
 */
public class AdministrationEffects {
    private final double taxCollectionModifier;
    private final double corruptionGainModifier;

    public AdministrationEffects(double taxCollectionModifier, double corruptionGainModifier) {
        this.taxCollectionModifier = taxCollectionModifier;
        this.corruptionGainModifier = corruptionGainModifier;
    }

    public double getTaxCollectionModifier() {
        return taxCollectionModifier;
    }

    public double getCorruptionGainModifier() {
        return corruptionGainModifier;
    }
}