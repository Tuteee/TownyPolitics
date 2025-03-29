package com.orbismc.townyPolitics.initialization;

import org.bukkit.plugin.PluginManager;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.hooks.*;
import com.orbismc.townyPolitics.listeners.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class ListenerInitializer {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public ListenerInitializer(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "ListenerInit");
    }

    public void initialize() {
        PluginManager pm = plugin.getServer().getPluginManager();

        // Register core event listeners
        CoreTownyEventListener coreEventListener = new CoreTownyEventListener(plugin);
        pm.registerEvents(coreEventListener, plugin);

        // Register status screen listeners with components
        StatusScreenListener statusListener = new StatusScreenListener(plugin);
        pm.registerEvents(statusListener, plugin);

        // Create government event listener
        GovernmentEventListener governmentEventListener = new GovernmentEventListener(
                plugin, plugin.getGovManager(), plugin.getTownGovManager());
        pm.registerEvents(governmentEventListener, plugin);

        // Create corruption event listener
        CorruptionEventListener corruptionEventListener = new CorruptionEventListener(
                plugin, plugin.getCorruptionManager(), plugin.getTownCorruptionManager(), plugin.getPPManager());
        pm.registerEvents(corruptionEventListener, plugin);

        // Create policy event listener
        PolicyEventListener policyEventListener = new PolicyEventListener(plugin, plugin.getPolicyManager());
        pm.registerEvents(policyEventListener, plugin);

        // Transaction handlers
        TransactionEmbezzlementHandler embezzlementHandler = new TransactionEmbezzlementHandler(plugin);
        pm.registerEvents(embezzlementHandler, plugin);
        logger.info("Transaction Embezzlement Handler registered");

        // Diagnostic handler for debugging
        DiagnosticTransactionHandler diagnosticHandler = new DiagnosticTransactionHandler(plugin);
        pm.registerEvents(diagnosticHandler, plugin);
        logger.info("Diagnostic Transaction Handler registered");

        // Town economy hook
        TownEconomyHook townEconomyHook = new TownEconomyHook(plugin);
        pm.registerEvents(townEconomyHook, plugin);
        logger.info("Town Economy Hook registered");

        logger.info("All event listeners registered");
    }
}