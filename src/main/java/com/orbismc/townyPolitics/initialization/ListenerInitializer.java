package com.orbismc.townyPolitics.initialization;

import com.orbismc.townyPolitics.components.ElectionStatusComponent; // Added import
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

        // Register status screen listener - Modified
        // The StatusScreenListener itself will now handle initializing components
        StatusScreenListener statusListener = new StatusScreenListener(plugin);
        pm.registerEvents(statusListener, plugin);
        logger.info("Status Screen Listener registered");


        // Individual listeners for specific features - REMOVED as StatusScreenListener handles components now
        // GovernmentEventListener governmentEventListener = new GovernmentEventListener(
        //        plugin, plugin.getGovManager(), plugin.getTownGovManager());
        // pm.registerEvents(governmentEventListener, plugin);

        // CorruptionEventListener corruptionEventListener = new CorruptionEventListener(
        //         plugin, plugin.getCorruptionManager(), plugin.getTownCorruptionManager(), plugin.getPPManager());
        // pm.registerEvents(corruptionEventListener, plugin);

        // PolicyEventListener policyEventListener = new PolicyEventListener(plugin, plugin.getPolicyManager());
        // pm.registerEvents(policyEventListener, plugin);

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

        // Budget-related listeners
        MilitaryStrengthListener militaryListener = new MilitaryStrengthListener(plugin);
        InfrastructureCostListener infraListener = new InfrastructureCostListener(plugin);
        pm.registerEvents(militaryListener, plugin);
        pm.registerEvents(infraListener, plugin);
        logger.info("Budget system listeners registered");

        logger.info("All event listeners registered");
    }
}