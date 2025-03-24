package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Intercepts tax payment transactions to apply corruption penalties
 */
public class TownyTaxTransactionHandler implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final TownyAPI townyAPI;

    public TownyTaxTransactionHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
        this.townyAPI = TownyAPI.getInstance();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTownyTransaction(TownyPreTransactionEvent event) {
        Transaction transaction = event.getTransaction();
        Account receivingAccount = transaction.getReceivingAccount();

        // Debug log
        plugin.getLogger().info("Transaction intercepted: " + transaction.getAmount() +
                " to " + (receivingAccount != null ? receivingAccount.getName() : "unknown") +
                ", type: " + transaction.getType());

        // We only want to modify deposits to nation accounts
        if (transaction.getAmount() <= 0 || !isNationAccount(receivingAccount)) {
            return;
        }

        // Try to get the nation object
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            plugin.getLogger().info("Could not find nation for account " + receivingAccount.getName());
            return;
        }

        // Get the corruption level and calculate penalty
        double corruption = corruptionManager.getCorruption(nation);

        // Calculate tax reduction: 5% per 1% corruption
        // At 20% corruption, tax becomes 0%
        double penaltyMultiplier = Math.max(0.0, 1.0 - (corruption * 0.05));

        // Only process if there's a penalty to apply
        if (penaltyMultiplier < 1.0) {
            double originalAmount = transaction.getAmount();
            double reducedAmount = originalAmount * penaltyMultiplier;

            // Log the reduction
            plugin.getLogger().info(String.format(
                    "Nation %s (corruption: %.1f%%) - tax reduced from %.2f to %.2f (%.1f%% penalty)",
                    nation.getName(),
                    corruption,
                    originalAmount,
                    reducedAmount,
                    (1.0 - penaltyMultiplier) * 100
            ));

            if (reducedAmount > 0) {
                try {
                    // Try to modify the transaction amount using reflection
                    modifyTransactionAmount(transaction, reducedAmount);
                    plugin.getLogger().info("Successfully modified transaction amount");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to modify transaction: " + e.getMessage());

                    // Fallback: cancel this transaction and create a new one
                    event.setCancelled(true);

                    try {
                        // Directly deposit the reduced amount
                        boolean success = receivingAccount.deposit(reducedAmount, "Corruption-adjusted tax");
                        if (success) {
                            plugin.getLogger().info("Successfully deposited reduced amount via fallback method");
                        } else {
                            plugin.getLogger().severe("Failed to deposit reduced amount via fallback method");
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Error in fallback deposit: " + ex.getMessage());
                    }
                }
            } else {
                // If amount would be zero, just cancel the transaction
                event.setCancelled(true);
                plugin.getLogger().info("Tax transaction cancelled due to high corruption level");
            }
        }
    }

    /**
     * Uses reflection to modify the amount in a Transaction object
     *
     * @param transaction The transaction to modify
     * @param newAmount The new amount to set
     * @throws Exception If reflection fails
     */
    private void modifyTransactionAmount(Transaction transaction, double newAmount) throws Exception {
        // Get the amount field by reflection
        Field amountField = Transaction.class.getDeclaredField("amount");
        amountField.setAccessible(true);

        // Update the amount
        amountField.set(transaction, newAmount);
    }

    /**
     * Check if the account belongs to a nation
     *
     * @param account The account to check
     * @return true if it's a nation account
     */
    private boolean isNationAccount(Account account) {
        if (account == null) return false;

        // Check for nation account pattern
        String name = account.getName();
        if (name == null) return false;

        // Log for debugging
        plugin.getLogger().info("Checking account: " + name);

        // Try getting UUID - TownyEconomy method
        UUID uuid = TownyEconomyHandler.getTownyObjectUUID(name);
        if (uuid != null) {
            Nation nation = townyAPI.getNation(uuid);
            return nation != null;
        }

        // Fallback check - pattern matching
        return name.toLowerCase().startsWith("nation-");
    }

    /**
     * Extract the nation from an account
     *
     * @param account The account
     * @return The nation or null if not found
     */
    private Nation getNationFromAccount(Account account) {
        if (account == null) return null;

        String name = account.getName();
        if (name == null) return null;

        // First try using Towny UUID method
        UUID uuid = TownyEconomyHandler.getTownyObjectUUID(name);
        if (uuid != null) {
            Nation nation = townyAPI.getNation(uuid);
            if (nation != null) {
                return nation;
            }
        }

        // Fallback to name-based lookup
        if (name.toLowerCase().startsWith("nation-")) {
            String nationName = name.substring(7); // Remove "nation-" prefix
            return townyAPI.getNation(nationName);
        }

        return null;
    }
}