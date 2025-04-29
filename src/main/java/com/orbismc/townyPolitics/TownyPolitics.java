package com.orbismc.townyPolitics;

import com.orbismc.townyPolitics.election.ElectionManager; // Added import
import com.orbismc.townyPolitics.handlers.PolicyEffectsHandler;
import com.orbismc.townyPolitics.initialization.CommandInitializer;
import com.orbismc.townyPolitics.initialization.ListenerInitializer;
import com.orbismc.townyPolitics.initialization.ManagerInitializer;
import com.orbismc.townyPolitics.initialization.StorageInitializer;
import com.palmergames.bukkit.towny.TownyAPI;
import com.orbismc.townyPolitics.managers.*;
import com.orbismc.townyPolitics.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyPolitics extends JavaPlugin implements DailyProcessor {

    private TownyAPI townyAPI;
    private ConfigManager configManager;
    private DebugLogger debugLogger;
    private DatabaseManager dbManager;

    // Managers
    private PoliticalPowerManager ppManager;
    private GovernmentManager govManager;
    private CorruptionManager corruptionManager;
    private TownGovernmentManager townGovManager;
    private TownCorruptionManager townCorruptionManager;
    private TownPoliticalPowerManager townPpManager;
    private TaxationManager taxationManager;
    private PolicyManager policyManager;
    private BudgetManager budgetManager;
    private EffectsManager effectsManager;
    private PolicyEffectsHandler policyEffectsHandler;
    private ElectionManager electionManager; // Added declaration

    @Override
    public void onEnable() {
        // Check dependencies
        if (!checkDependencies()) {
            return;
        }

        townyAPI = TownyAPI.getInstance();

        // Initialize core components
        initializeCoreComponents();

        // Initialize storage - DatabaseManager must be initialized first
        // StorageInitializer's constructor might need dbManager, ensure order
        StorageInitializer storageInitializer = new StorageInitializer(this, dbManager);
        storageInitializer.initialize();

        // Initialize managers
        ManagerInitializer managerInitializer = new ManagerInitializer(this);
        managerInitializer.initialize();

        // Get the initialized managers
        this.ppManager = managerInitializer.getPpManager();
        this.govManager = managerInitializer.getGovManager();
        this.corruptionManager = managerInitializer.getCorruptionManager();
        this.townGovManager = managerInitializer.getTownGovManager();
        this.townCorruptionManager = managerInitializer.getTownCorruptionManager();
        this.townPpManager = managerInitializer.getTownPpManager();
        this.taxationManager = managerInitializer.getTaxationManager();
        this.policyManager = managerInitializer.getPolicyManager();
        this.budgetManager = managerInitializer.getBudgetManager();
        this.effectsManager = managerInitializer.getEffectsManager();
        this.electionManager = managerInitializer.getElectionManager(); // Added assignment
        this.policyEffectsHandler = new PolicyEffectsHandler(this);

        // Register event listeners
        ListenerInitializer listenerInitializer = new ListenerInitializer(this);
        listenerInitializer.initialize();

        // Register commands
        CommandInitializer commandInitializer = new CommandInitializer(this);
        commandInitializer.initialize();

        debugLogger.info("TownyPolitics has been enabled");
        getLogger().info("TownyPolitics has been enabled");
    }

    private void initializeCoreComponents() {
        // Initialize config and debug logger
        configManager = new ConfigManager(this);
        debugLogger = new DebugLogger(this);
        debugLogger.info("TownyPolitics debug logger initialized");

        // Initialize database
        dbManager = new DatabaseManager(this);
        debugLogger.info("Database manager initialized");
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny not found! Disabling TownyPolitics...");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        // Stop election task first
        if (electionManager != null) {
            electionManager.stopUpdateTask(); // Added stopping the task
        }

        // Save all data on plugin disable
        saveAllData();

        // Close database connections
        if (dbManager != null) {
            dbManager.close();
        }

        debugLogger.info("TownyPolitics has been disabled");
        getLogger().info("TownyPolitics has been disabled");
    }

    private void saveAllData() {
        if (ppManager != null) ppManager.saveAllData();
        if (govManager != null) govManager.saveAllData();
        if (corruptionManager != null) corruptionManager.saveAllData();
        if (townGovManager != null) townGovManager.saveAllData();
        if (townCorruptionManager != null) townCorruptionManager.saveAllData();
        if (townPpManager != null) townPpManager.saveAllData();
        if (policyManager != null) policyManager.saveAllData();
        if (budgetManager != null) budgetManager.saveAllData();
        if (electionManager != null) electionManager.saveAllData(); // Added election manager save
    }

    public void reload() {
        // Reload configuration
        getConfigManager().loadConfig(); // Use getter for safety

        // Reinitialize debugLogger with new config settings
        debugLogger = new DebugLogger(this);
        debugLogger.info("Debug logger reinitialized with new config settings");

        // Reload all managers
        if (ppManager != null) ppManager.loadData();
        if (govManager != null) govManager.loadData();
        if (corruptionManager != null) corruptionManager.loadData();
        if (townGovManager != null) townGovManager.loadData();
        if (townCorruptionManager != null) townCorruptionManager.loadData();
        if (townPpManager != null) townPpManager.loadData();
        if (policyManager != null) policyManager.reload();
        if (budgetManager != null) budgetManager.loadData();
        if (electionManager != null) electionManager.loadData(); // Added election manager reload

        debugLogger.info("Configuration reloaded");
    }

    @Override
    public void processNewDay() {
        debugLogger.info("Processing daily updates");

        // Use a null-safe approach when calling
        if (ppManager != null) ppManager.processNewDay();
        if (corruptionManager != null) corruptionManager.processNewDay();
        if (townCorruptionManager != null) townCorruptionManager.processNewDay();
        if (townPpManager != null) townPpManager.processNewDay();
        if (policyManager != null) policyManager.processNewDay();
        if (budgetManager != null) budgetManager.processBudgetCycle();
        // Election manager uses its own timer, not the daily processor

        debugLogger.info("Daily updates complete");
    }

    // Getters
    public TownyAPI getTownyAPI() { return townyAPI; }
    public ConfigManager getConfigManager() { return configManager; }
    public PoliticalPowerManager getPPManager() { return ppManager; }
    public GovernmentManager getGovManager() { return govManager; }
    public CorruptionManager getCorruptionManager() { return corruptionManager; }
    public TownGovernmentManager getTownGovManager() { return townGovManager; }
    public TownCorruptionManager getTownCorruptionManager() { return townCorruptionManager; }
    public TownPoliticalPowerManager getTownPPManager() { return townPpManager; }
    public TaxationManager getTaxationManager() { return taxationManager; }
    public PolicyManager getPolicyManager() { return policyManager; }
    public DatabaseManager getDatabaseManager() { return dbManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public BudgetManager getBudgetManager() { return budgetManager; }
    public EffectsManager getEffectsManager() { return effectsManager; }
    public PolicyEffectsHandler getPolicyEffectsHandler() { return policyEffectsHandler; }
    public ElectionManager getElectionManager() { return electionManager; } // Added getter
}