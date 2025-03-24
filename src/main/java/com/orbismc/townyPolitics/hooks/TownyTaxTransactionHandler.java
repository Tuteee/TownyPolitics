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
        plugin.getLogger().info("TownyTaxTransactionHandler initialized");
    }

    @EventHandler(priority = EventPriority.LOWEST) // Changed to LOWEST to intercept before modifications
    public void onTownyTransaction(TownyPreTransactionEvent event) {
        plugin.getLogger().info("=== Transaction event fired ===");
        Transaction transaction = event.getTransaction();
        Account receivingAccount = transaction.getReceivingAccount();

        // Enhanced debug log
        plugin.getLogger().info("Transaction details: Amount=" + transaction.getAmount() +
                ", Type=" + transaction.getType() +
                ", Receiver=" + (receivingAccount != null ? receivingAccount.getName() : "null") +
                ", HasSender=" + transaction.hasSenderAccount());

        if (transaction.hasSenderAccount()) {
            plugin.getLogger().info("Sender account: " + transaction.getSendingAccount().getName());
        }

        // We only want to modify deposits to nation accounts
        if (transaction.getAmount() <= 0) {
            plugin.getLogger().info("Skipping: amount <= 0");
            return;
        }

        if (receivingAccount == null) {
            plugin.getLogger().info("Skipping: receiving account is null");
            return;
        }

        boolean isNationAccount = isNationAccount(receivingAccount);
        plugin.getLogger().info("Is nation account: " + isNationAccount);

        if (!isNationAccount) {
            return;
        }

        // Try to get the nation object
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            plugin.getLogger().info("Skipping: could not find nation for account " + receivingAccount.getName());
            return;
        }

        plugin.getLogger().info("Found nation: " + nation.getName());

        // Get the corruption level and calculate penalty
        double corruption = corruptionManager.getCorruption(nation);
        plugin.getLogger().info("Nation corruption level: " + corruption + "%");

        // Calculate tax reduction: 5% per 1% corruption
        // At 20% corruption, tax becomes 0%
        double penaltyMultiplier = Math.max(0.0, 1.0 - (corruption * 0.05));
        plugin.getLogger().info("Calculated penalty multiplier: " + penaltyMultiplier);

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

                    // Verify the change took effect
                    Field amountField = Transaction.class.getDeclaredField("amount");
                    amountField.setAccessible(true);
                    double newAmountCheck = (double) amountField.get(transaction);
                    plugin.getLogger().info("Verified new amount: " + newAmountCheck);

                    plugin.getLogger().info("Successfully modified transaction amount via reflection");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to modify transaction: " + e.getMessage());
                    e.printStackTrace();

                    // Fallback: cancel this transaction and create a new one
                    plugin.getLogger().info("Trying fallback approach: cancel and deposit");
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
                        ex.printStackTrace();
                    }
                }
            } else {
                // If amount would be zero, just cancel the transaction
                event.setCancelled(true);
                plugin.getLogger().info("Tax transaction cancelled due to high corruption level");
            }
        } else {
            plugin.getLogger().info("No penalty applied: penalty multiplier = " + penaltyMultiplier);
        }

        plugin.getLogger().info("=== Transaction processing complete ===");
    }

    /**
     * Uses reflection to modify the amount in a Transaction object
     *
     * @param transaction The transaction to modify
     * @param newAmount The new amount to set
     * @throws Exception If reflection fails
     */
    private void modifyTransactionAmount(Transaction transaction, double newAmount) throws Exception {
        plugin.getLogger().info("Attempting to modify transaction amount via reflection");

        // Get the amount field by reflection
        Field amountField = Transaction.class.getDeclaredField("amount");
        plugin.getLogger().info("Found amount field: " + amountField);

        amountField.setAccessible(true);
        plugin.getLogger().info("Set field accessible");

        // Update the amount
        amountField.set(transaction, newAmount);
        plugin.getLogger().info("Set new amount: " + newAmount);
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
        plugin.getLogger().info("Checking if account is nation account: " + name);

        // Try getting UUID - TownyEconomy method
        UUID uuid = TownyEconomyHandler.getTownyObjectUUID(name);
        plugin.getLogger().info("Account UUID from TownyEconomyHandler: " + uuid);

        if (uuid != null) {
            Nation nation = townyAPI.getNation(uuid);
            boolean isNation = nation != null;
            plugin.getLogger().info("Nation found by UUID: " + isNation);
            return isNation;
        }

        // Fallback check - pattern matching
        boolean matchesPattern = name.toLowerCase().startsWith("nation-");
        plugin.getLogger().info("Account name matches 'nation-' pattern: " + matchesPattern);
        return matchesPattern;
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

        plugin.getLogger().info("Getting nation from account: " + name);

        // First try using Towny UUID method
        UUID uuid = TownyEconomyHandler.getTownyObjectUUID(name);
        plugin.getLogger().info("Account UUID: " + uuid);

        if (uuid != null) {
            Nation nation = townyAPI.getNation(uuid);
            if (nation != null) {
                plugin.getLogger().info("Found nation by UUID: " + nation.getName());
                return nation;
            }
        }

        // Fallback to name-based lookup
        if (name.toLowerCase().startsWith("nation-")) {
            String nationName = name.substring(7); // Remove "nation-" prefix
            plugin.getLogger().info("Trying name-based lookup for: " + nationName);
            Nation nation = townyAPI.getNation(nationName);
            if (nation != null) {
                plugin.getLogger().info("Found nation by name: " + nation.getName());
            } else {
                plugin.getLogger().info("Nation not found by name");
            }
            return nation;
        }

        plugin.getLogger().info("Could not find any nation for this account");
        return null;
    }
}