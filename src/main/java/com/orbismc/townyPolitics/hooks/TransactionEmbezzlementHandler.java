package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refined embezzlement handler that intercepts transactions to nation accounts
 * and applies embezzlement based on corruption levels
 */
public class TransactionEmbezzlementHandler implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final TownyAPI townyAPI;
    private final DebugLogger debugLogger;

    // Transaction tracking to prevent double embezzlement
    private final Map<String, Long> recentTransactions = new ConcurrentHashMap<>();

    // Cooldown period in milliseconds (5 seconds)
    private static final long COOLDOWN_PERIOD = 5000L;

    // Whether to send nation messages about embezzlement
    private final boolean sendNationMessages;

    public TransactionEmbezzlementHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
        this.townyAPI = TownyAPI.getInstance();
        this.debugLogger = plugin.getDebugLogger();

        // Get config setting for notifications
        this.sendNationMessages = plugin.getConfig().getBoolean("corruption.notifications.embezzlement_message", true);

        // Schedule cleanup task for transaction tracking
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupTransactions, 6000L, 6000L); // Run every 5 minutes

        debugLogger.info("Refined Transaction Embezzlement Handler initialized");
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

        // Get receiving account
        Account receivingAccount;
        try {
            receivingAccount = transaction.getReceivingAccount();
            if (receivingAccount == null) {
                return;
            }
        } catch (Exception e) {
            debugLogger.warning("Error getting receiving account: " + e.getMessage());
            return;
        }

        String accountName = receivingAccount.getName();
        if (accountName == null) {
            return;
        }

        // Only process nation accounts
        if (!accountName.startsWith("nation-")) {
            return;
        }

        // Extract nation from account
        Nation nation = getNationFromAccount(receivingAccount);
        if (nation == null) {
            debugLogger.info("Could not determine nation for account: " + accountName);
            return;
        }

        // Get corruption level
        double corruption = corruptionManager.getCorruption(nation);

        // Skip if no corruption
        if (corruption <= 0) {
            return;
        }

        // Generate a unique transaction identifier
        String transactionId = accountName + ":" + amount + ":" + System.currentTimeMillis();

        // Check if this is a duplicate transaction (prevent double embezzlement)
        if (isDuplicateTransaction(transactionId)) {
            debugLogger.info("Skipping duplicate transaction to " + accountName);
            return;
        }

        // Track this transaction
        trackTransaction(transactionId);

        // Calculate embezzlement rate based on corruption level
        double embezzleRate = calculateEmbezzlementRate(corruption);
        double embezzledAmount = amount * embezzleRate;

        // Don't embezzle tiny amounts
        if (embezzledAmount <= 0.01) {
            return;
        }

        debugLogger.info("EMBEZZLEMENT: Nation " + nation.getName() + " with corruption " +
                String.format("%.1f", corruption) + "% will have " +
                String.format("%.2f", embezzledAmount) + " embezzled (" +
                String.format("%.1f", embezzleRate * 100) + "% of " +
                String.format("%.2f", amount) + ")");

        // Check if nation has enough money before attempting to withdraw
        if (nation.getAccount().getHoldingBalance() < embezzledAmount) {
            debugLogger.warning("Nation " + nation.getName() +
                    " doesn't have enough funds for embezzlement. Available: " +
                    nation.getAccount().getHoldingBalance() + ", Needed: " + embezzledAmount);
            return;
        }

        // Schedule a task to withdraw the embezzled funds after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean success = nation.getAccount().withdraw(embezzledAmount,
                    "Funds embezzled due to corruption (" + String.format("%.1f", corruption) + "%)");

            if (success) {
                debugLogger.info("SUCCESS: " +
                        String.format("%.2f", embezzledAmount) +
                        " was embezzled from " + nation.getName() + " due to corruption");

                // Send message to nation if enabled
                if (sendNationMessages) {
                    try {
                        // Format the message
                        String message = ChatColor.DARK_RED + String.format("%.2f", embezzledAmount) +
                                ChatColor.RED + " was embezzled from the treasury due to corruption (" +
                                String.format("%.1f", corruption) + "%)!";

                        // Send to nation players directly using TownyAPI
                        // This avoids using Nation-specific methods that might not be in your version
                        for (org.bukkit.entity.Player player : TownyAPI.getInstance().getOnlinePlayers(nation)) {
                            player.sendMessage(message);
                        }
                    } catch (Exception e) {
                        debugLogger.warning("Could not send embezzlement message: " + e.getMessage());
                    }
                }
            } else {
                debugLogger.warning("FAILED: Could not withdraw " +
                        embezzledAmount + " from " + nation.getName());
            }
        }, 2L); // 2 tick delay to ensure transaction is complete
    }

    /**
     * Calculate embezzlement rate based on corruption level
     * Higher corruption = higher percentage of taxes lost to corruption
     *
     * @param corruption The corruption level (0-100)
     * @return Embezzlement rate (0.0-1.0)
     */
    private double calculateEmbezzlementRate(double corruption) {
        // Get configuration thresholds
        double lowThreshold = plugin.getConfig().getDouble("corruption.thresholds.low", 25.0);
        double mediumThreshold = plugin.getConfig().getDouble("corruption.thresholds.medium", 50.0);
        double highThreshold = plugin.getConfig().getDouble("corruption.thresholds.high", 75.0);
        double criticalThreshold = plugin.getConfig().getDouble("corruption.thresholds.critical", 90.0);

        // Get embezzlement rates from config (or use defaults)
        double lowRate = plugin.getConfig().getDouble("corruption.embezzlement.low", 0.05);
        double mediumRate = plugin.getConfig().getDouble("corruption.embezzlement.medium", 0.15);
        double highRate = plugin.getConfig().getDouble("corruption.embezzlement.high", 0.30);
        double criticalRate = plugin.getConfig().getDouble("corruption.embezzlement.critical", 0.60);

        // Determine rate based on corruption level
        if (corruption >= criticalThreshold) {
            return criticalRate;
        } else if (corruption >= highThreshold) {
            return highRate;
        } else if (corruption >= mediumThreshold) {
            return mediumRate;
        } else if (corruption >= lowThreshold) {
            return lowRate;
        } else {
            // Below the low threshold, scale linearly from 0 to lowRate
            return (corruption / lowThreshold) * lowRate;
        }
    }

    /**
     * Check if a transaction is a duplicate (already processed)
     *
     * @param transactionId Unique transaction identifier
     * @return True if this is a duplicate transaction
     */
    private boolean isDuplicateTransaction(String transactionId) {
        // Extract key parts for matching (account and amount)
        String[] parts = transactionId.split(":");
        if (parts.length < 2) {
            return false;
        }

        String matchKey = parts[0] + ":" + parts[1];

        // Check for recent transactions with same account and amount
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : recentTransactions.entrySet()) {
            if (entry.getKey().startsWith(matchKey) &&
                    (currentTime - entry.getValue() < COOLDOWN_PERIOD)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Track a transaction to prevent double processing
     *
     * @param transactionId Unique transaction identifier
     */
    private void trackTransaction(String transactionId) {
        recentTransactions.put(transactionId, System.currentTimeMillis());
    }

    /**
     * Clean up old transaction records to prevent memory leaks
     */
    private void cleanupTransactions() {
        long currentTime = System.currentTimeMillis();
        recentTransactions.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > COOLDOWN_PERIOD);
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