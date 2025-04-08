package com.orbismc.townyPolitics.budget;

/**
 * Class to encapsulate all military budget effects
 */
public class MilitaryEffects {
    private final double strengthModifier;
    private final double buildingDamageModifier;

    public MilitaryEffects(double strengthModifier, double buildingDamageModifier) {
        this.strengthModifier = strengthModifier;
        this.buildingDamageModifier = buildingDamageModifier;
    }

    public double getStrengthModifier() {
        return strengthModifier;
    }

    public double getBuildingDamageModifier() {
        return buildingDamageModifier;
    }
}