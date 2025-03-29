package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.object.Town;
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
     * Apply policy effects to additional claimed blocks bonus
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownClaim(TownClaimEvent event) {
        Town town = event.getTown();

        // For future implementation if Towny adds better events.
        // For now, just log the claim event.
        logger.fine("Town " + town.getName() + " claimed a new town block");

        // Example of potential future implementation when Towny adds more events:
        // Get combined policy effects
        PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(town);

        // Apply town block cost modifier if an appropriate method becomes available
        double costModifier = effects.getTownBlockCostModifier();
        if (costModifier != 1.0) {
            logger.fine("Town " + town.getName() + " has town block cost modifier: " +
                    String.format("%.1f%%", (costModifier - 1.0) * 100));
        }
    }
}