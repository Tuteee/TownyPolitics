package com.orbismc.townyPolitics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.commands.*;
import com.orbismc.townyPolitics.hooks.TransactionEmbezzlementHandler;
import com.orbismc.townyPolitics.hooks.DiagnosticTransactionHandler;
import com.orbismc.townyPolitics.hooks.TownEconomyHook;
import com.orbismc.townyPolitics.listeners.GovernmentEventListener;
import com.orbismc.townyPolitics.listeners.CorruptionEventListener;
import com.orbismc.townyPolitics.listeners.PolicyEventListener;
import com.orbismc.townyPolitics.managers.*;
import com.orbismc.townyPolitics.storage.*;
import com.orbismc.townyPolitics.storage.mysql.*;
import com.orbismc.townyPolitics.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyPolitics extends JavaPlugin implements DailyProcessor {

    private TownyAPI townyAPI;
    private ConfigManager configManager;
    private DebugLogger debugLogger;
    private DatabaseManager dbManager;

    // Storage interfaces
    private IPoliticalPowerStorage ppStorage;
    private IGovernmentStorage govStorage;
    private ICorruptionStorage corruptionStorage;
    private ITownGovernmentStorage townGovStorage;
    private ITownCorruptionStorage townCorruptionStorage;
    private ITownPoliticalPowerStorage townPpStorage;
    private IPolicyStorage policyStorage;

    // Managers
    private PoliticalPowerManager ppManager;
    private GovernmentManager govManager;
    private CorruptionManager corruptionManager;
    private TownGovernmentManager townGovManager;
    private TownCorruptionManager townCorruptionManager;
    private TownPoliticalPowerManager townPpManager;
    private TaxationManager taxationManager;
    private PolicyManager policyManager;

    // Event listeners
    private GovernmentEventListener governmentEventListener;
    private CorruptionEventListener corruptionEventListener;
    private PolicyEventListener policyEventListener;

    @Override
    public void onEnable() {
        // Check if Towny is loaded
        if (!checkDependencies()) {
            return;
        }

        townyAPI = TownyAPI.getInstance();

        // Initialize config and debug logger
        configManager = new ConfigManager(this);
        debugLogger = new DebugLogger(this);
        debugLogger.info("TownyPolitics debug logger initialized");

        // Initialize database and storage
        setupDatabase();
        setupStorage();

        // Initialize managers
        initializeManagers();

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();

        debugLogger.info("TownyPolitics has been enabled");
        getLogger().info("TownyPolitics has been enabled");
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny not found! Disabling TownyPolitics...");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    private void setupDatabase() {
        dbManager = new DatabaseManager(this);
        debugLogger.info("Database manager initialized");
    }

    private void setupStorage() {
        // Initialize MySQL storage implementations
        ppStorage = new MySQLPoliticalPowerStorage(this, dbManager);
        govStorage = new MySQLGovernmentStorage(this, dbManager);
        corruptionStorage = new MySQLCorruptionStorage(this, dbManager);
        townGovStorage = new MySQLTownGovernmentStorage(this, dbManager);
        townCorruptionStorage = new MySQLTownCorruptionStorage(this, dbManager);
        townPpStorage = new MySQLTownPoliticalPowerStorage(this, dbManager);
        policyStorage = new MySQLPolicyStorage(this, dbManager);

        debugLogger.info("MySQL storage initialized");
    }

    private void initializeManagers() {
        // Initialize nation managers
        govManager = new GovernmentManager(this, govStorage);
        ppManager = new PoliticalPowerManager(this, ppStorage, govManager);
        corruptionManager = new CorruptionManager(this, corruptionStorage, govManager);

        // Initialize town managers
        townGovManager = new TownGovernmentManager(this, townGovStorage);
        townCorruptionManager = new TownCorruptionManager(this, townCorruptionStorage, townGovManager);
        townPpManager = new TownPoliticalPowerManager(this, townPpStorage, townGovManager);
        debugLogger.info("Town Political Power Manager initialized");

        // Initialize taxation manager
        taxationManager = new TaxationManager(this, corruptionManager, townCorruptionManager);

        // Initialize policy manager
        policyManager = new PolicyManager(this, policyStorage, govManager, townGovManager);
        debugLogger.info("Policy Manager initialized");

        debugLogger.info("All managers initialized");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        // Create event listeners
        governmentEventListener = new GovernmentEventListener(this, govManager, townGovManager);
        corruptionEventListener = new CorruptionEventListener(this, corruptionManager, townCorruptionManager, ppManager);
        policyEventListener = new PolicyEventListener(this, policyManager);

        // Register listeners
        pm.registerEvents(governmentEventListener, this);
        pm.registerEvents(corruptionEventListener, this);
        pm.registerEvents(policyEventListener, this);

        // Transaction handlers
        TransactionEmbezzlementHandler embezzlementHandler = new TransactionEmbezzlementHandler(this);
        pm.registerEvents(embezzlementHandler, this);
        debugLogger.info("Transaction Embezzlement Handler registered");

        // Diagnostic handler for debugging
        DiagnosticTransactionHandler diagnosticHandler = new DiagnosticTransactionHandler(this);
        pm.registerEvents(diagnosticHandler, this);
        debugLogger.info("Diagnostic Transaction Handler registered");

        // Town economy hook
        TownEconomyHook townEconomyHook = new TownEconomyHook(this);
        pm.registerEvents(townEconomyHook, this);
        debugLogger.info("Town Economy Hook registered");

        debugLogger.info("All event listeners registered");
    }

    private void registerCommands() {
        try {
            // Create command executors for nations
            GovernmentCommand nationGovCommand = new GovernmentCommand(this, govManager, "nation");
            OverviewCommand nationOverviewCommand = new OverviewCommand(this, govManager, ppManager, corruptionManager);
            CorruptionCommand nationCorruptionCommand = new CorruptionCommand(this, corruptionManager, ppManager);
            PoliticalPowerCommand ppCommand = new PoliticalPowerCommand(this, ppManager);

            // Create command executors for towns
            GovernmentCommand townGovCommand = new GovernmentCommand(this, govManager, "town");
            TownPoliticalPowerCommand townPpCommand = new TownPoliticalPowerCommand(this, townPpManager);

            // Create command executors for policies
            PolicyCommand townPolicyCommand = new PolicyCommand(this, policyManager, "town");
            PolicyCommand nationPolicyCommand = new PolicyCommand(this, policyManager, "nation");

            // Register nation commands
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "government", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "gov", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "overview", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "o", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "corruption", nationCorruptionCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "pp", ppCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "policy", nationPolicyCommand);

            // Register town commands
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "government", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "gov", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "policy", townPolicyCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "pp", townPpCommand);

            // Register TownyAdmin command
            new TownyAdminPoliticsCommand(this, govManager, ppManager, corruptionManager);

            // Register test command for embezzlement
            TestEmbezzlementCommand testEmbezzlementCommand = new TestEmbezzlementCommand(this);
            this.getCommand("taxtest").setExecutor(testEmbezzlementCommand);

            // Register migration command
            new MigrationCommand(this);

            debugLogger.info("All commands registered");
        } catch (Exception e) {
            debugLogger.severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
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
        if (ppStorage != null) {
            ppStorage.saveAll();
        }
        if (govStorage != null) {
            govStorage.saveAll();
        }
        if (corruptionStorage != null) {
            corruptionStorage.saveAll();
        }
        if (townGovStorage != null) {
            townGovStorage.saveAll();
        }
        if (townCorruptionStorage != null) {
            townCorruptionStorage.saveAll();
        }
        if (townPpStorage != null) {
            townPpStorage.saveAll();
        }
        if (policyStorage != null) {
            policyStorage.saveAll();
        }
    }

    public void reload() {
        // Reload configuration
        configManager.loadConfig();

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
        if (corruptionManager != null) {
            corruptionManager.loadData();
        }

        // Reload town data
        if (townGovManager != null) {
            townGovManager.loadData();
        }
        if (townCorruptionManager != null) {
            townCorruptionManager.loadData();
        }
        if (townPpManager != null) {
            townPpManager.loadData();
        }

        // Reload policy data
        if (policyManager != null) {
            policyManager.reload();
        }

        debugLogger.info("Configuration reloaded");
    }

    @Override
    public void processNewDay() {
        debugLogger.info("Processing daily updates");

        // Process daily updates for all components
        ppManager.processNewDay();
        corruptionManager.processNewDay();
        townCorruptionManager.processNewDay();
        townPpManager.processNewDay();
        policyManager.processNewDay();

        debugLogger.info("Daily updates complete");
    }

    // Getters
    public TownyAPI getTownyAPI() {
        return townyAPI;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PoliticalPowerManager getPPManager() {
        return ppManager;
    }

    public GovernmentManager getGovManager() {
        return govManager;
    }

    public CorruptionManager getCorruptionManager() {
        return corruptionManager;
    }

    public TownGovernmentManager getTownGovManager() {
        return townGovManager;
    }

    public TownCorruptionManager getTownCorruptionManager() {
        return townCorruptionManager;
    }

    public TownPoliticalPowerManager getTownPPManager() {
        return townPpManager;
    }

    public TaxationManager getTaxationManager() {
        return taxationManager;
    }

    public PolicyManager getPolicyManager() {
        return policyManager;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public DebugLogger getDebugLogger() {
        return debugLogger;
    }
}