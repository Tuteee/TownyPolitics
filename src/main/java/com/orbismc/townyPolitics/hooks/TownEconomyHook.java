package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.TownBlockClaimCostCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Enhanced TownEconomyHook to apply policy effects to town economy events
 */
public class TownEconomyHook implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public TownEconomyHook(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "TownEconomy");
        logger.info("Town Economy Hook initialized");
    }

    /**
     * Apply policy effects to town upkeep calculations
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownUpkeepCalculation(TownUpkeepCalculationEvent event) {
        Town town = event.getTown();
        double currentUpkeep = event.getUpkeep();

        // Get policy upkeep modifier
        double upkeepModifier = plugin.getPolicyEffectsHandler().getUpkeepModifier(town);

        // Get budget upkeep modifier if available
        double budgetModifier = 1.0;
        if (plugin.getEffectsManager() != null) {
            budgetModifier = plugin.getEffectsManager().getTownInfrastructureEffects(town).getUpkeepModifier();
        }

        // Apply combined modifier
        double combinedModifier = upkeepModifier * budgetModifier;

        if (combinedModifier != 1.0) {
            double newUpkeep = currentUpkeep * combinedModifier;
            event.setUpkeep(newUpkeep);

            logger.fine("Modified upkeep for town " + town.getName() + " from " +
                    currentUpkeep + " to " + newUpkeep +
                    " (policy: " + upkeepModifier + ", budget: " + budgetModifier + ")");
        }
    }

    /**
     * Apply policy effects to town claim costs
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownBlockClaimCostCalculation(TownBlockClaimCostCalculationEvent event) {
        Town town = event.getTown();
        double currentPrice = event.getPrice();

        // Get policy town block cost modifier
        double policyModifier = plugin.getPolicyEffectsHandler().getTownBlockCostModifier(town);

        // Get budget town block cost modifier if available
        double budgetModifier = 1.0;
        if (plugin.getEffectsManager() != null) {
            budgetModifier = plugin.getEffectsManager().getTownInfrastructureEffects(town).getClaimCostModifier();
        }

        // Apply combined modifier
        double combinedModifier = policyModifier * budgetModifier;

        if (combinedModifier != 1.0) {
            double newPrice = currentPrice * combinedModifier;
            event.setPrice(newPrice);

            logger.fine("Modified town claim price for " + town.getName() + " from " +
                    currentPrice + " to " + newPrice +
                    " (policy: " + policyModifier + ", budget: " + budgetModifier + ")");
        }
    }
}