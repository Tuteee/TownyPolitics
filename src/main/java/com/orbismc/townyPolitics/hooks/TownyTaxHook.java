package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.TaxationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Hooks into Towny tax-related commands to apply corruption modifiers
 */
public class TownyTaxHook implements Listener {

    private final TownyPolitics plugin;
    private final TaxationManager taxationManager;
    private final CorruptionManager corruptionManager;

    public TownyTaxHook(TownyPolitics plugin) {
        this.plugin = plugin;
        this.taxationManager = plugin.getTaxationManager();
        this.corruptionManager = plugin.getCorruptionManager();
    }

    /**
     * Intercept commands to set town taxes
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        // Only process tax-setting commands
        if (!command.startsWith("/town set tax") &&
                !command.startsWith("/t set tax") &&
                !command.startsWith("/town settax") &&
                !command.startsWith("/t settax")) {
            return;
        }

        // Get the player
        CommandSender sender = event.getPlayer();

        // Get the tax amount being set
        String[] parts = command.split("\\s+");
        if (parts.length < 4) {
            return; // Not enough arguments
        }

        double newTax;
        try {
            newTax = Double.parseDouble(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return; // Not a valid number
        }

        // Get the player's town
        Town town = null;
        try {
            com.palmergames.bukkit.towny.TownyAPI townyAPI = com.palmergames.bukkit.towny.TownyAPI.getInstance();
            town = townyAPI.getResidentTownOrNull(townyAPI.getResident(event.getPlayer().getUniqueId()));
        } catch (Exception e) {
            return; // Not in a town
        }

        if (town == null || !town.hasNation()) {
            return;
        }

        try {
            Nation nation = town.getNation();
            boolean isTaxPercentage = town.isTaxPercentage();

            // Get the modified max tax
            double modifiedTaxLimit = taxationManager.getModifiedMaxTaxRate(town, isTaxPercentage);

            // Check if new tax exceeds the modified limit
            if (newTax > modifiedTaxLimit) {
                // Cancel the event
                event.setCancelled(true);

                // Inform player about the corruption-based limit
                double corruptionModifier = corruptionManager.getTaxationModifier(nation);
                String modifierStr = String.format("%+.1f%%", corruptionModifier * 100);

                if (corruptionModifier > 0) {
                    // Positive modifier - tax limit increased
                    sender.sendMessage(
                            ChatColor.YELLOW + "Your nation's corruption has increased the maximum tax to " +
                                    String.format("%.2f%s", modifiedTaxLimit, isTaxPercentage ? "%" : "") +
                                    " (" + modifierStr + " modifier)"
                    );
                } else if (corruptionModifier < 0) {
                    // Negative modifier - tax limit decreased
                    sender.sendMessage(
                            ChatColor.RED + "Your nation's corruption has decreased the maximum tax to " +
                                    String.format("%.2f%s", modifiedTaxLimit, isTaxPercentage ? "%" : "") +
                                    " (" + modifierStr + " modifier)"
                    );
                }

                sender.sendMessage(
                        ChatColor.RED + "The tax you tried to set (" +
                                String.format("%.2f%s", newTax, isTaxPercentage ? "%" : "") +
                                ") exceeds this limit."
                );
            }

            // For percentage taxes, also check the max tax amount cap
            if (isTaxPercentage && town.isTaxPercentage()) {
                double modifiedMaxAmount = taxationManager.getModifiedMaxTaxPercentAmount(town);

                // We're not canceling for this, just informing
                sender.sendMessage(
                        ChatColor.YELLOW + "Note: Your percentage tax is capped at a maximum of " +
                                String.format("%.2f", modifiedMaxAmount) + " per resident."
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying corruption modifier to taxes: " + e.getMessage());
        }
    }
}