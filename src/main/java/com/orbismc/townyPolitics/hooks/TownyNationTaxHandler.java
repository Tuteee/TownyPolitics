package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.time.dailytaxes.PreTownPaysNationTaxEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Directly intercepts nation tax payments and applies corruption penalties
 */
public class TownyNationTaxHandler implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;

    public TownyNationTaxHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNationTaxPayment(PreTownPaysNationTaxEvent event) {
        Nation nation = event.getNation();
        double taxAmount = event.getTax();

        // Log original tax amount
        plugin.getLogger().info(String.format("Nation tax payment intercepted: %s is about to pay %.2f to %s",
                event.getTown().getName(), taxAmount, nation.getName()));

        // Get corruption level
        double corruption = corruptionManager.getCorruption(nation);

        // For every 1% corruption, reduce tax by 5%
        // At 20% corruption, tax becomes 0%
        double penaltyMultiplier = Math.max(0.0, 1.0 - (corruption * 0.05));

        // Calculate reduced amount
        double reducedAmount = taxAmount * penaltyMultiplier;

        // Log the reduction
        plugin.getLogger().info(String.format(
                "Nation %s (corruption: %.1f%%) - tax reduced from %.2f to %.2f (%.1f%% penalty)",
                nation.getName(),
                corruption,
                taxAmount,
                reducedAmount,
                (1.0 - penaltyMultiplier) * 100
        ));

        if (reducedAmount > 0) {
            // Set the modified tax amount
            event.setTax(reducedAmount);
            plugin.getLogger().info("Modified tax amount set to " + reducedAmount);
        } else {
            // Cancel the tax payment entirely
            event.setCancelled(true);
            event.setCancelMessage("Tax payment cancelled due to high corruption level");
            plugin.getLogger().info("Tax payment cancelled due to high corruption level");
        }
    }
}