package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.TownBlockClaimCostCalculationEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.InfrastructureEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class InfrastructureCostListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public InfrastructureCostListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "InfrastructureListener");

        logger.info("Infrastructure Cost Listener initialized");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownBlockClaimCost(TownBlockClaimCostCalculationEvent event) {
        Town town = event.getTown();
        double currentPrice = event.getPrice();

        // Get infrastructure effects
        InfrastructureEffects townEffects = plugin.getEffectsManager().getTownInfrastructureEffects(town);
        double townModifier = townEffects.getClaimCostModifier();

        // If town has a nation, also check nation modifier
        double nationModifier = 1.0;
        if (town.hasNation()) {
            try {
                InfrastructureEffects nationEffects = plugin.getEffectsManager()
                        .getNationInfrastructureEffects(town.getNation());
                nationModifier = nationEffects.getClaimCostModifier();
            } catch (Exception e) {
                logger.warning("Error getting nation effects: " + e.getMessage());
            }
        }

        // Use the better modifier (lower cost)
        double finalModifier = Math.min(townModifier, nationModifier);

        // Only apply if there's an actual modification
        if (finalModifier != 1.0) {
            double newPrice = currentPrice * finalModifier;
            event.setPrice(newPrice);

            logger.fine("Modified claim price for " + town.getName() +
                    " from " + currentPrice + " to " + newPrice +
                    " (mod: " + finalModifier + ")");
        }
    }
}