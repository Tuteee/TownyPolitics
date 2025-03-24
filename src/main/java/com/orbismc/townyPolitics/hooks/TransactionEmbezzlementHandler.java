package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Post-transaction embezzlement handler based on TownyTransactionEvent
 */
public class TransactionEmbezzlementHandler implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final TownyAPI townyAPI;

    public TransactionEmbezzlementHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
        this.townyAPI = TownyAPI.getInstance();
        plugin.getLogger().info("Transaction Embezzlement Handler initialized");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTownyTransaction(TownyTransactionEvent event) {
        Transaction transaction = event.getTransaction();

        // Only process ADD transactions
        if (transaction.getType() != TransactionType.ADD) {
            return;
        }

        // Skip small amounts
        double amount = transaction.getAmount();
        if (amount < 0.01) {
            return;
        }

        // Get receiving account name - we need to check if it's a nation account
        Account receivingAccount = null;
        try {
            receivingAccount = transaction.getReceivingAccount();
        } catch (Exception e) {
            return; // Method not found or other error
        }

        if (receivingAccount == null) {
            return;
        }

        String accountName = receivingAccount.getName();
        if (accountName == null || !accountName.startsWith("nation-")) {
            return; // Not a nation account
        }

        plugin.getLogger().info("EMBEZZLEMENT: Detected transaction to nation account: " + accountName + " for " + amount);

        // Extract nation from account
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            plugin.getLogger().info("EMBEZZLEMENT: Could not determine nation for account: " + accountName);
            return;
        }

        // Get corruption and calculate embezzlement
        double corruption = corruptionManager.getCorruption(nation);
        double embezzleRate = Math.min(1.0, corruption / 100.0);
        double embezzledAmount = amount * embezzleRate;

        if (embezzledAmount <= 0.01) {
            return; // Not enough to embezzle
        }

        plugin.getLogger().info("EMBEZZLEMENT: Nation " + nation.getName() + " with corruption " + corruption +
                "% will have " + embezzledAmount + " embezzled");

        // Schedule a task to withdraw the embezzled funds after the transaction completes
        // Use a 1-tick delay to ensure the transaction is fully completed
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean success = nation.getAccount().withdraw(embezzledAmount,
                    "Funds embezzled due to corruption (" + corruption + "%)");

            if (success) {
                plugin.getLogger().info("EMBEZZLEMENT SUCCESS: " + embezzledAmount +
                        " was embezzled from " + nation.getName() + " due to corruption");
            } else {
                plugin.getLogger().warning("EMBEZZLEMENT FAILED: Could not withdraw " +
                        embezzledAmount + " from " + nation.getName());
            }
        }, 1L);
    }

    /**
     * Extract the nation from a nation account
     */
    private Nation getNationFromAccount(Account account) {
        if (account == null) return null;

        String name = account.getName();
        if (name == null) return null;

        // Try using UUID method first
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