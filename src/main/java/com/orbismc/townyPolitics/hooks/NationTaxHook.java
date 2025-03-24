package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.time.dailytaxes.PreTownPaysNationTaxEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.TaxationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Hooks into Towny nation tax payment events to simulate embezzlement due to corruption
 */
public class NationTaxHook implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final TaxationManager taxationManager;

    public NationTaxHook(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
        this.taxationManager = plugin.getTaxationManager();
        plugin.getLogger().info("NationTaxHook initialized with debug logging");
    }

    /**
     * Intercept nation tax payment events to simulate embezzlement
     * Town pays full amount, but nation only receives a portion based on corruption
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNationTaxPayment(PreTownPaysNationTaxEvent event) {
        Town town = event.getTown();
        Nation nation = event.getNation();
        double originalAmount = event.getTax();

        plugin.getLogger().info("DEBUG: NationTaxHook processing tax payment from " + town.getName() + " to " + nation.getName());
        plugin.getLogger().info("DEBUG: Original tax amount: " + originalAmount);

        // Skip if no tax to pay
        if (originalAmount <= 0) {
            plugin.getLogger().info("DEBUG: Tax amount is zero or negative, skipping");
            return;
        }

        // Get corruption level
        double corruption = corruptionManager.getCorruption(nation);
        plugin.getLogger().info("DEBUG: Nation corruption level: " + corruption + "%");

        // Get embezzlement percentage based on corruption
        double embezzleRate = taxationManager.getTaxPenaltyModifier(nation);
        plugin.getLogger().info("DEBUG: Embezzlement rate: " + (embezzleRate * 100) + "%");

        // Calculate embezzled amount
        double embezzledAmount = originalAmount * embezzleRate;
        double effectiveAmount = originalAmount - embezzledAmount;

        plugin.getLogger().info("DEBUG: Embezzled amount: " + embezzledAmount);
        plugin.getLogger().info("DEBUG: Effective amount: " + effectiveAmount);

        // Log the transaction
        plugin.getLogger().info(String.format(
                "Town %s paying %.2f tax to Nation %s (Corruption: %.1f%%)",
                town.getName(), originalAmount, nation.getName(), corruption
        ));

        if (embezzledAmount > 0) {
            plugin.getLogger().info(String.format(
                    "%.2f (%.1f%%) embezzled due to corruption! Nation only receives %.2f",
                    embezzledAmount, embezzleRate * 100, effectiveAmount
            ));

            if (effectiveAmount <= 0) {
                // Everything is embezzled
                plugin.getLogger().info("DEBUG: All tax money embezzled, trying alternative approach");

                try {
                    // Force the town to pay but don't give nation anything
                    boolean success = town.getAccount().withdraw(originalAmount, "Tax payment to " + nation.getName() + " (embezzled due to corruption)");
                    plugin.getLogger().info("DEBUG: Manual town withdrawal success: " + success);

                    // Cancel the event to prevent normal processing
                    event.setCancelled(true);

                    // Set a custom message
                    event.setCancelMessage("Tax paid to corrupt officials! (No money reached the nation treasury)");
                    plugin.getLogger().info("DEBUG: Event cancelled with custom message: " + event.getCancelMessage());
                } catch (Exception e) {
                    plugin.getLogger().severe("Error handling embezzled tax: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Set the reduced amount the nation will receive
                plugin.getLogger().info("DEBUG: Setting reduced tax amount to: " + effectiveAmount);
                event.setTax(effectiveAmount);
            }
        }

        plugin.getLogger().info("DEBUG: Tax event processing complete. Event cancelled: " + event.isCancelled());
    }
}