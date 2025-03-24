package com.orbismc.townyPolitics.hooks;

import org.bukkit.Bukkit;
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

import java.util.UUID;

/**
 * Intercepts transactions to nation accounts and applies embezzlement based on corruption levels
 */
public class CorruptionEmbezzlementHandler implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final TownyAPI townyAPI;

    public CorruptionEmbezzlementHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
        this.townyAPI = TownyAPI.getInstance();
        plugin.getLogger().info("Corruption Embezzlement Handler initialized");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreTransaction(TownyPreTransactionEvent event) {
        Transaction transaction = event.getTransaction();

        // Only target ADD transactions to nation accounts
        if (transaction.getType() != TransactionType.ADD) return;

        Account receivingAccount = transaction.getReceivingAccount();
        if (receivingAccount == null) return;

        String accountName = receivingAccount.getName();
        if (!accountName.startsWith("nation-")) return;

        // Debug logs
        plugin.getLogger().info("Corruption detection: Transaction to " + accountName +
                " for " + transaction.getAmount() + " detected");

        // Get the nation from the account
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            plugin.getLogger().info("Could not find nation for account: " + accountName);
            return;
        }

        // Get corruption level
        double corruption = corruptionManager.getCorruption(nation);
        double embezzleRate = Math.min(1.0, corruption / 100.0);
        double originalAmount = transaction.getAmount();

        plugin.getLogger().info("Nation " + nation.getName() + " has corruption level of " +
                corruption + "% (embezzle rate: " + (embezzleRate * 100) + "%)");

        if (embezzleRate > 0) {
            double embezzledAmount = originalAmount * embezzleRate;
            double effectiveAmount = originalAmount - embezzledAmount;

            // Tax is still collected from town but reduced/eliminated for nation
            plugin.getLogger().info(String.format("Embezzlement: %.2f (%.1f%%) of %.2f is being embezzled from %s",
                    embezzledAmount, embezzleRate * 100, originalAmount, nation.getName()));

            if (embezzleRate >= 0.99) {
                // Complete embezzlement - cancel the transaction
                event.setCancelled(true);
                plugin.getLogger().info("Transaction cancelled due to 100% embezzlement. Nation receives nothing.");

                // Custom message to display in-game
                try {
                    // Use reflection to set a custom message if available
                    if (event.getClass().getMethod("setCancelMessage", String.class) != null) {
                        event.getClass().getMethod("setCancelMessage", String.class)
                                .invoke(event, "Tax collected but completely embezzled due to corruption!");
                        plugin.getLogger().info("Set custom cancel message");
                    }
                } catch (Exception e) {
                    // If method doesn't exist, just log it
                    plugin.getLogger().info("Custom cancel message not supported");
                }
            } else if (embezzleRate > 0.01) {
                // Partial embezzlement - town pays full, nation receives reduced amount
                plugin.getLogger().info("Nation will only receive " + effectiveAmount +
                        " (original: " + originalAmount + ")");

                // Schedule a task to withdraw the embezzled amount right after this transaction
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Withdraw the embezzled amount after the transaction completes
                    boolean success = nation.getAccount().withdraw(embezzledAmount,
                            "Funds embezzled due to corruption (" + corruption + "%)");

                    if (success) {
                        plugin.getLogger().info("Successfully embezzled " + embezzledAmount +
                                " from nation " + nation.getName());
                    } else {
                        plugin.getLogger().warning("Failed to embezzle funds from nation " +
                                nation.getName());
                    }
                }, 1L); // 1 tick delay to ensure it runs after the transaction completes
            }
        }
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
            Nation nation = townyAPI.getNation(nationName);
            return nation;
        }

        return null;
    }
}