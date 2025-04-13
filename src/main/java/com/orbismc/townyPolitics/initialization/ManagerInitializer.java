package com.orbismc.townyPolitics.initialization;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.*;
import com.orbismc.townyPolitics.storage.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class ManagerInitializer {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    // Managers
    private PoliticalPowerManager ppManager;
    private GovernmentManager govManager;
    private CorruptionManager corruptionManager;
    private TownGovernmentManager townGovManager;
    private TownCorruptionManager townCorruptionManager;
    private TownPoliticalPowerManager townPpManager;
    private TaxationManager taxationManager;
    private PolicyManager policyManager;
    private BudgetManager budgetManager; // Added missing declaration
    private EffectsManager effectsManager; // Added missing declaration

    public ManagerInitializer(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "ManagerInit");
    }

    public void initialize() {
        StorageInitializer storageInit = new StorageInitializer(plugin, plugin.getDatabaseManager());
        storageInit.initialize();

        // Initialize nation managers
        govManager = new GovernmentManager(plugin, storageInit.getGovStorage());
        ppManager = new PoliticalPowerManager(plugin, storageInit.getPpStorage(), govManager);
        corruptionManager = new CorruptionManager(plugin, storageInit.getCorruptionStorage(), govManager);

        // Initialize town managers
        townGovManager = new TownGovernmentManager(plugin, storageInit.getTownGovStorage());
        townCorruptionManager = new TownCorruptionManager(plugin, storageInit.getTownCorruptionStorage(), townGovManager);
        townPpManager = new TownPoliticalPowerManager(plugin, storageInit.getTownPpStorage(), townGovManager);
        logger.info("Town Political Power Manager initialized");

        // Initialize taxation manager
        taxationManager = new TaxationManager(plugin, corruptionManager);

        // Initialize policy manager
        policyManager = new PolicyManager(plugin, storageInit.getPolicyStorage(), govManager, townGovManager);
        logger.info("Policy Manager initialized");

        // Initialize budget manager - using the correct constructor signature
        budgetManager = new BudgetManager(plugin);
        logger.info("Budget Manager initialized");

        // Initialize effects manager - using the correct constructor signature
        effectsManager = new EffectsManager(plugin);
        logger.info("Effects Manager initialized");

        logger.info("All managers initialized");
    }

    // Getters for all the managers
    public PoliticalPowerManager getPpManager() { return ppManager; }
    public GovernmentManager getGovManager() { return govManager; }
    public CorruptionManager getCorruptionManager() { return corruptionManager; }
    public TownGovernmentManager getTownGovManager() { return townGovManager; }
    public TownCorruptionManager getTownCorruptionManager() { return townCorruptionManager; }
    public TownPoliticalPowerManager getTownPpManager() { return townPpManager; }
    public TaxationManager getTaxationManager() { return taxationManager; }
    public PolicyManager getPolicyManager() { return policyManager; }
    public BudgetManager getBudgetManager() { return budgetManager; } // Added missing getter
    public EffectsManager getEffectsManager() { return effectsManager; } // Added missing getter
}