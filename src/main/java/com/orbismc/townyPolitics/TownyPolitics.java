package com.orbismc.townyPolitics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.commands.*;
import com.orbismc.townyPolitics.hooks.TransactionEmbezzlementHandler;
import com.orbismc.townyPolitics.hooks.DiagnosticTransactionHandler;
import com.orbismc.townyPolitics.listeners.TownyEventListener;
import com.orbismc.townyPolitics.managers.*;
import com.orbismc.townyPolitics.storage.*;
import com.orbismc.townyPolitics.storage.mysql.*;
import com.orbismc.townyPolitics.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyPolitics extends JavaPlugin {

    private TownyAPI townyAPI;

    // Nation managers
    private PoliticalPowerManager ppManager;
    private GovernmentManager govManager;
    private CorruptionManager nationCorruptionManager;

    // Town managers
    private TownGovernmentManager townGovManager;
    private TownCorruptionManager townCorruptionManager;

    // Common managers
    private TaxationManager taxationManager;

    // Storage interfaces
    private IPoliticalPowerStorage ppStorage;
    private IGovernmentStorage govStorage;
    private ICorruptionStorage nationCorruptionStorage;
    private ITownGovernmentStorage townGovStorage;
    private ITownCorruptionStorage townCorruptionStorage;

    // Listeners and utilities
    private TownyEventListener eventListener;
    private DatabaseManager dbManager;
    private DebugLogger debugLogger;

    @Override
    public void onEnable() {
        // Check if Towny is loaded
        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny not found! Disabling TownyPolitics...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        townyAPI = TownyAPI.getInstance();

        // Save default config
        saveDefaultConfig();

        // Initialize debug logger
        debugLogger = new DebugLogger(this);
        debugLogger.info("TownyPolitics debug logger initialized");

        // Initialize database manager
        dbManager = new DatabaseManager(this);

        // Initialize storages based on configuration
        if (dbManager.isUsingMySQL()) {
            // Using MySQL
            debugLogger.info("Using MySQL for data storage");
            ppStorage = new MySQLPoliticalPowerStorage(this, dbManager);
            govStorage = new MySQLGovernmentStorage(this, dbManager);
            nationCorruptionStorage = new MySQLCorruptionStorage(this, dbManager);
            townGovStorage = new MySQLTownGovernmentStorage(this, dbManager);
            townCorruptionStorage = new MySQLTownCorruptionStorage(this, dbManager);
        } else {
            // Using YAML
            debugLogger.info("Using YAML for data storage");
            ppStorage = new YamlPoliticalPowerStorage(this);
            govStorage = new YamlGovernmentStorage(this);
            nationCorruptionStorage = new YamlCorruptionStorage(this);
            townGovStorage = new YamlTownGovernmentStorage(this);
            townCorruptionStorage = new YamlTownCorruptionStorage(this);
        }

        // Initialize nation managers
        ppManager = new PoliticalPowerManager(this, ppStorage);
        govManager = new GovernmentManager(this, govStorage);
        nationCorruptionManager = new CorruptionManager(this, nationCorruptionStorage, govManager);

        // Initialize town managers
        townGovManager = new TownGovernmentManager(this, townGovStorage);
        townCorruptionManager = new TownCorruptionManager(this, townCorruptionStorage);

        // Initialize taxation manager (depends on corruption manager)
        taxationManager = new TaxationManager(this, nationCorruptionManager);

        // Initialize and register listener
        eventListener = new TownyEventListener(this, ppManager, nationCorruptionManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Register transaction handlers
        TransactionEmbezzlementHandler embezzlementHandler = new TransactionEmbezzlementHandler(this);
        getServer().getPluginManager().registerEvents(embezzlementHandler, this);
        debugLogger.info("Registered Transaction Embezzlement Handler");

        // Register diagnostic handler for debugging
        DiagnosticTransactionHandler diagnosticHandler = new DiagnosticTransactionHandler(this);
        getServer().getPluginManager().registerEvents(diagnosticHandler, this);
        debugLogger.info("Registered Diagnostic Transaction Handler");

        // Connect listener and manager (circular reference)
        ppManager.setEventListener(eventListener);

        // Register commands
        registerCommands();

        debugLogger.info("TownyPolitics has been enabled!");
        getLogger().info("TownyPolitics has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data on plugin disable
        if (ppStorage != null) {
            ppStorage.saveAll();
        }
        if (govStorage != null) {
            govStorage.saveAll();
        }
        if (nationCorruptionStorage != null) {
            nationCorruptionStorage.saveAll();
        }
        if (townGovStorage != null) {
            townGovStorage.saveAll();
        }
        if (townCorruptionStorage != null) {
            townCorruptionStorage.saveAll();
        }

        // Close database connections
        if (dbManager != null) {
            dbManager.close();
        }

        debugLogger.info("TownyPolitics has been disabled!");
        getLogger().info("TownyPolitics has been disabled!");
    }

    public void reload() {
        reloadConfig();

        // Reinitialize debugLogger with new config settings
        debugLogger = new DebugLogger(this);
        debugLogger.info("Debug logger reinitialized with new config settings");

        // Reload nation data
        if (ppManager != null) {
            ppManager.loadData();
        }
        if (govManager != null) {
            govManager.loadData();
        }
        if (nationCorruptionManager != null) {
            nationCorruptionManager.loadData();
        }

        // Reload town data
        if (townGovManager != null) {
            townGovManager.loadData();
        }
        if (townCorruptionManager != null) {
            townCorruptionManager.loadData();
        }

        debugLogger.info("Configuration reloaded");
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        try {
            // Create command executors for nations
            GovernmentCommand nationGovCommand = new GovernmentCommand(this, govManager, "nation");
            OverviewCommand nationOverviewCommand = new OverviewCommand(this, govManager, ppManager, nationCorruptionManager);
            CorruptionCommand nationCorruptionCommand = new CorruptionCommand(this, nationCorruptionManager, ppManager);
            PoliticalPowerCommand ppCommand = new PoliticalPowerCommand(this, ppManager);

            // Create command executors for towns
            GovernmentCommand townGovCommand = new GovernmentCommand(this, govManager, "town");

            // Register nation commands
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "government", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "gov", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "overview", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "o", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "corruption", nationCorruptionCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "pp", ppCommand);

            // Register town commands
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "government", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "gov", townGovCommand);

            // Register TownyAdmin command
            new TownyAdminPoliticsCommand(this, govManager, ppManager, nationCorruptionManager);

            // Register test command for embezzlement
            TestEmbezzlementCommand testEmbezzlementCommand = new TestEmbezzlementCommand(this);
            this.getCommand("taxtest").setExecutor(testEmbezzlementCommand);

            // Register migration command
            new MigrationCommand(this);

            debugLogger.info("Successfully registered all commands");
        } catch (Exception e) {
            debugLogger.severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public TownyAPI getTownyAPI() {
        return townyAPI;
    }

    public PoliticalPowerManager getPPManager() {
        return ppManager;
    }

    public GovernmentManager getGovManager() {
        return govManager;
    }

    public CorruptionManager getCorruptionManager() {
        return nationCorruptionManager;
    }

    public TownGovernmentManager getTownGovManager() {
        return townGovManager;
    }

    public TownCorruptionManager getTownCorruptionManager() {
        return townCorruptionManager;
    }

    public TaxationManager getTaxationManager() {
        return taxationManager;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public DebugLogger getDebugLogger() {
        return debugLogger;
    }
}