package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.economy.NationTransactionEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Intercepts completed transactions to nation accounts and implements embezzlement
 */
public class PostTransactionEmbezzlementHandler implements Listener {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;

    public PostTransactionEmbezzlementHandler(TownyPolitics plugin) {
        this.plugin = plugin;
        this.corruptionManager = plugin.getCorruptionManager();
        plugin.getLogger().info("Post-Transaction Embezzlement Handler initialized");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationTransaction(NationTransactionEvent event) {
        Transaction transaction = event.getTransaction();
        Nation nation = event.getNation();

        // Only process deposits (ADD transactions)
        if (transaction.getType() != TransactionType.ADD) {
            return;
        }

        // Skip very small amounts
        double amount = transaction.getAmount();
        if (amount < 0.01) {
            return;
        }

        // Check if this looks like a tax payment
        Account senderAccount = null;
        try {
            if (transaction.hasSenderAccount()) {
                senderAccount = transaction.getSendingAccount();
            }
        } catch (NoSuchMethodError e) {
            // Method doesn't exist in this version of Towny
            plugin.getLogger().info("hasSenderAccount method not available, assuming tax payment");
        }

        boolean isTaxPayment = true;
        if (senderAccount != null) {
            String senderName = senderAccount.getName();
            isTaxPayment = senderName != null && senderName.startsWith("town-");
        }

        if (!isTaxPayment) {
            return;
        }

        // Get corruption level and calculate embezzlement
        double corruption = corruptionManager.getCorruption(nation);
        double embezzleRate = Math.min(1.0, corruption / 100.0);
        double embezzledAmount = amount * embezzleRate;

        if (embezzledAmount <= 0.01) {
            return; // Skip if nothing significant to embezzle
        }

        plugin.getLogger().info(String.format(
                "POST-TRANSACTION EMBEZZLEMENT: Nation %s received %.2f, embezzling %.2f (%.1f%%) due to corruption",
                nation.getName(), amount, embezzledAmount, embezzleRate * 100
        ));

        // Remove the embezzled amount from the nation's account
        boolean success = nation.getAccount().withdraw(embezzledAmount,
                "Funds embezzled due to corruption (" + corruption + "%)");

        if (success) {
            plugin.getLogger().info(String.format(
                    "Successfully embezzled %.2f from nation %s. Nation only keeps %.2f",
                    embezzledAmount, nation.getName(), amount - embezzledAmount
            ));
        } else {
            plugin.getLogger().warning(String.format(
                    "Failed to embezzle %.2f from nation %s. Nation may have insufficient funds.",
                    embezzledAmount, nation.getName()
            ));
        }
    }
}