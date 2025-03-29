package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.TownBlockClaimCostCalculationEvent;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotClaimEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Hooks into Towny's economy events to apply town policy modifiers
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

        // Get combined policy effects
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);

        // Apply upkeep modifier
        double upkeepModifier = effects.getUpkeepModifier();
        if (upkeepModifier != 1.0) {
            double newUpkeep = currentUpkeep * upkeepModifier;
            event.setUpkeep(newUpkeep);

            logger.fine("Modified upkeep for town " + town.getName() + " from " +
                    currentUpkeep + " to " + newUpkeep +
                    " (" + (upkeepModifier > 1.0 ? "+" : "") +
                    String.format("%.1f%%", (upkeepModifier - 1.0) * 100) + ")");
        }
    }

    /**
     * Apply policy effects to town claim costs
     * This event is fired before a claim is processed and allows modifying the price
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownBlockClaimCostCalculation(TownBlockClaimCostCalculationEvent event) {
        Town town = event.getTown();
        double currentPrice = event.getPrice();

        // Get combined policy effects
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);

        // Apply town block cost modifier
        double costModifier = effects.getTownBlockCostModifier();
        if (costModifier != 1.0) {
            double newPrice = currentPrice * costModifier;
            event.setPrice(newPrice);

            logger.fine("Modified town claim price for " + town.getName() + " from " +
                    currentPrice + " to " + newPrice +
                    " (" + (costModifier > 1.0 ? "+" : "") +
                    String.format("%.1f%%", (costModifier - 1.0) * 100) + ")");
        }
    }

    /**
     * Monitor pre-claim events for potential policy effects
     * This doesn't modify the price (that's done in TownBlockClaimCostCalculationEvent)
     * but allows for monitoring and potentially cancelling claims based on policies
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownPreClaim(TownPreClaimEvent event) {
        Town town = event.getTown();

        // Get combined policy effects
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);

        // Example: Log that a pre-claim is happening
        logger.fine("Town " + town.getName() + " is attempting to claim land with TownBlockCostModifier: " +
                String.format("%.1f%%", (effects.getTownBlockCostModifier() - 1.0) * 100));

        // You could add policy effects that potentially cancel claims based on certain conditions
        // For example:
        /*
        if (effects.hasPolicyEffect("no_claims_during_war") && town.isAtWar()) {
            event.setCancelled(true);
            event.setCancelMessage("Cannot claim land during war due to town policy");
        }
        */
    }

    /**
     * Monitor successful town claims
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownClaim(TownClaimEvent event) {
        Town town = event.getTown();
        if (town == null)
            return;

        // Get combined policy effects
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);

        // Just log that a claim was successful
        logger.fine("Town " + town.getName() + " successfully claimed land with TownBlockCostModifier: " +
                String.format("%.1f%%", (effects.getTownBlockCostModifier() - 1.0) * 100));
    }

    /**
     * Handle plot claims
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlotClaim(PlotClaimEvent event) {
        TownBlock townBlock = event.getTownBlock();
        if (townBlock == null || !townBlock.hasTown())
            return;

        Town town = townBlock.getTownOrNull();
        if (town == null)
            return;

        // Get combined policy effects
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);

        // Log that plot ownership changed
        logger.fine("Plot claimed in town " + town.getName() +
                " with PlotCostModifier: " +
                String.format("%.1f%%", (effects.getPlotCostModifier() - 1.0) * 100));
    }
}