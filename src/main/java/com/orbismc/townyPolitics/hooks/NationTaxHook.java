package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.NationPreTaxPaymentEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.TaxationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Hooks into Towny nation tax payment event to apply corruption penalties
 */
public class NationTaxHook implements Listener {

    private final TownyPolitics plugin;
    private final TaxationManager taxationManager;
    private final CorruptionManager corruptionManager;

    public NationTaxHook(TownyPolitics plugin) {
        this.plugin = plugin;
        this.taxationManager = plugin.getTaxationManager();
        this.corruptionManager = plugin.getCorruptionManager();
    }

    /**
     * Intercept nation tax payment events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onNationTaxPayment(NationPreTaxPaymentEvent event) {
        Town town = event.getTown();
        Nation nation = event.getNation();
        double originalAmount = event.getAmount();

        // Log the original payment
        plugin.getLogger().info(String.format("Nation tax payment intercepted: %s is about to pay %.2f to %s",
                town.getName(), originalAmount, nation.getName()));

        // Get corruption level and apply tax penalties
        double corruption = corruptionManager.getCorruption(nation);
        double penaltyModifier = taxationManager.getTaxPenaltyModifier(nation);

        if (penaltyModifier > 0) {
            // Calculate the embezzled amount (money that's lost to corruption)
            double embezzledAmount = originalAmount * penaltyModifier;
            double receivedAmount = originalAmount - embezzledAmount;

            // Log the adjustment
            plugin.getLogger().info(String.format("Nation %s (corruption: %.1f%%) - tax embezzled: %.2f (%.1f%% of %.2f)",
                    nation.getName(), corruption, embezzledAmount, penaltyModifier * 100, originalAmount));

            // Town still pays the full amount, but nation only receives part of it
            if (embezzledAmount > 0) {
                // Let the town pay the full tax as normal
                if (embezzledAmount >= originalAmount) {
                    // If everything is embezzled, don't even credit the nation's account
                    plugin.getLogger().info("All tax money embezzled due to critical corruption level");
                    town.getAccount().withdraw(originalAmount, "Tax payment to " + nation.getName() + " (embezzled due to corruption)");
                    event.setCancelled(true);
                } else {
                    // Nation gets partial amount, the rest is "lost" to corruption
                    event.setAmount(receivedAmount);
                    plugin.getLogger().info(String.format("Nation only receives %.2f of %.2f tax payment (%.2f embezzled)",
                            receivedAmount, originalAmount, embezzledAmount));
                }
            }
        }
    }
}