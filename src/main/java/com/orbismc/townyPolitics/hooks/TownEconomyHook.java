package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.TownBlockClaimCostCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.InfrastructureEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Enhanced TownEconomyHook to apply infrastructure budget effects to town economy events
 */
public class TownEconomyHook implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public TownEconomyHook(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "TownEconomy");
        logger.info("Enhanced Town Economy Hook initialized");
    }

    /**
     * Apply budget effects to town upkeep calculations
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownUpkeepCalculation(TownUpkeepCalculationEvent event) {
        Town town = event.getTown();
        double currentUpkeep = event.getUpkeep();

        // Get policy upkeep modifier
        double policyModifier = plugin.getPolicyEffectsHandler().getUpkeepModifier(town);

        // Get budget upkeep modifier
        double budgetModifier = 1.0;
        if (plugin.getEffectsManager() != null) {
            InfrastructureEffects effects = plugin.getEffectsManager().getTownInfrastructureEffects(town);
            budgetModifier = effects.getUpkeepModifier();

            // Log the infrastructure budget allocation status
            if (budgetModifier < 0.9) {
                logger.fine("Town " + town.getName() + " has overfunded infrastructure: " + budgetModifier);
            } else if (budgetModifier > 1.1) {
                logger.fine("Town " + town.getName() + " has underfunded infrastructure: " + budgetModifier);
            }
        }

        // Apply combined modifier
        double combinedModifier = policyModifier * budgetModifier;

        if (combinedModifier != 1.0) {
            double newUpkeep = currentUpkeep * combinedModifier;
            event.setUpkeep(newUpkeep);

            // Notify town mayor if online
            notifyAboutModifier(town, "upkeep", currentUpkeep, newUpkeep, combinedModifier);

            logger.fine("Modified upkeep for town " + town.getName() + " from " +
                    currentUpkeep + " to " + newUpkeep +
                    " (policy: " + policyModifier + ", budget: " + budgetModifier + ")");
        }
    }

    /**
     * Apply budget effects to town claim costs
     * This is critical for infrastructure budget integration
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTownBlockClaimCostCalculation(TownBlockClaimCostCalculationEvent event) {
        Town town = event.getTown();
        double currentPrice = event.getPrice();
        Player player = event.getPlayer();

        // Get policy town block cost modifier
        double policyModifier = plugin.getPolicyEffectsHandler().getTownBlockCostModifier(town);

        // Get budget town block cost modifier
        double budgetModifier = 1.0;
        if (plugin.getEffectsManager() != null) {
            InfrastructureEffects effects = plugin.getEffectsManager().getTownInfrastructureEffects(town);
            budgetModifier = effects.getClaimCostModifier();

            // Enhanced infrastructure effect based on town size
            // Large towns get additional discount from infrastructure
            int townSize = town.getTownBlocks().size();
            if (townSize > 100 && budgetModifier < 1.0) {
                // Scale the modifier based on town size (larger towns get more benefit)
                double sizeBonus = Math.min(0.2, townSize / 1000.0); // Max 20% additional reduction
                budgetModifier = Math.max(0.6, budgetModifier - sizeBonus); // Don't go below 60%
                logger.fine("Town " + town.getName() + " gets additional claim discount due to size: " + sizeBonus);
            }
        }

        // Enhance with nation bonus if applicable
        double nationModifier = 1.0;
        if (town.hasNation()) {
            try {
                InfrastructureEffects nationEffects = plugin.getEffectsManager()
                        .getNationInfrastructureEffects(town.getNation());
                nationModifier = nationEffects.getClaimCostModifier();

                // Nation with good infrastructure can provide additional benefits
                if (nationModifier < budgetModifier) {
                    logger.fine("Town " + town.getName() + " benefits from nation infrastructure: " + nationModifier);
                }
            } catch (Exception e) {
                logger.warning("Error getting nation infrastructure effects: " + e.getMessage());
            }
        }

        // Use the better modifier (lower cost)
        double finalBudgetModifier = Math.min(budgetModifier, nationModifier);

        // Apply combined modifier
        double combinedModifier = policyModifier * finalBudgetModifier;

        if (combinedModifier != 1.0) {
            double newPrice = currentPrice * combinedModifier;
            event.setPrice(newPrice);

            // Send feedback to the player about cost modification
            if (player != null) {
                String modifierStr = String.format("%+.1f%%", (1.0 - combinedModifier) * 100);
                String direction = combinedModifier < 1.0 ? "reduced" : "increased";

                player.sendMessage(ChatColor.GOLD + "Town Infrastructure: " +
                        ChatColor.GREEN + "Claim cost " + direction + " by " + modifierStr);
            }

            logger.fine("Modified town claim price for " + town.getName() + " from " +
                    currentPrice + " to " + newPrice +
                    " (policy: " + policyModifier + ", budget: " + finalBudgetModifier + ")");
        }
    }

    /**
     * Notify town mayor about significant economic modifications
     */
    private void notifyAboutModifier(Town town, String type, double original, double modified, double modifier) {
        if (town.getMayor() == null || town.getMayor().getPlayer() == null) {
            return;
        }

        Player mayor = town.getMayor().getPlayer();

        // Only notify about significant changes (> 10%)
        if (Math.abs(1.0 - modifier) > 0.1) {
            String modifierStr = String.format("%+.1f%%", (1.0 - modifier) * 100);
            String direction = modifier < 1.0 ? "reduced" : "increased";

            mayor.sendMessage(ChatColor.GOLD + "Town Budget Effect: " +
                    ChatColor.GREEN + type + " " + direction + " by " + modifierStr);
        }
    }
}