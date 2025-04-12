package com.orbismc.townyPolitics.handlers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

/**
 * Centralizes application of policy effects to gameplay mechanics
 */
public class PolicyEffectsHandler {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public PolicyEffectsHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "PolicyEffects");
        logger.info("Policy Effects Handler initialized");
    }

    /**
     * Get the tax modifier for a town considering all active policies
     */
    public double getTaxModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getTaxModifier();
        logger.fine("Town " + town.getName() + " tax modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the trade modifier for a town considering all active policies
     */
    public double getTradeModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getTradeModifier();
        logger.fine("Town " + town.getName() + " trade modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the town block cost modifier for a town considering all active policies
     */
    public double getTownBlockCostModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getTownBlockCostModifier();
        logger.fine("Town " + town.getName() + " town block cost modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the plot cost modifier for a town considering all active policies
     */
    public double getPlotCostModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getPlotCostModifier();
        logger.fine("Town " + town.getName() + " plot cost modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the upkeep modifier for a town considering all active policies
     */
    public double getUpkeepModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getUpkeepModifier();
        logger.fine("Town " + town.getName() + " upkeep modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the corruption gain modifier for a town considering all active policies
     */
    public double getCorruptionGainModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getCorruptionGainModifier();
        logger.fine("Town " + town.getName() + " corruption gain modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the political power gain modifier for a town considering all active policies
     */
    public double getPoliticalPowerGainModifier(Town town) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);
        double modifier = effects.getPoliticalPowerGainModifier();
        logger.fine("Town " + town.getName() + " PP gain modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the tax modifier for a nation considering all active policies
     */
    public double getTaxModifier(Nation nation) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
        double modifier = effects.getTaxModifier();
        logger.fine("Nation " + nation.getName() + " tax modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the corruption gain modifier for a nation considering all active policies
     */
    public double getCorruptionGainModifier(Nation nation) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
        double modifier = effects.getCorruptionGainModifier();
        logger.fine("Nation " + nation.getName() + " corruption gain modifier from policies: " + modifier);
        return modifier;
    }

    /**
     * Get the political power gain modifier for a nation considering all active policies
     */
    public double getPoliticalPowerGainModifier(Nation nation) {
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
        double modifier = effects.getPoliticalPowerGainModifier();
        logger.fine("Nation " + nation.getName() + " PP gain modifier from policies: " + modifier);
        return modifier;
    }
}