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
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
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
    private PolicyManager policyManager;

    // Storage interfaces
    private IPoliticalPowerStorage ppStorage;
    private IGovernmentStorage govStorage;
    private ICorruptionStorage nationCorruptionStorage;
    private ITownGovernmentStorage townGovStorage;
    private ITownCorruptionStorage townCorruptionStorage;
    private IPolicyStorage policyStorage;

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

        // Save default config and update with new values
        loadAndUpdateConfig();

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
            policyStorage = new MySQLPolicyStorage(this, dbManager);
            debugLogger.info("Initialized MySQL Policy Storage");
        } else {
            // Using YAML
            debugLogger.info("Using YAML for data storage");
            ppStorage = new YamlPoliticalPowerStorage(this);
            govStorage = new YamlGovernmentStorage(this);
            nationCorruptionStorage = new YamlCorruptionStorage(this);
            townGovStorage = new YamlTownGovernmentStorage(this);
            townCorruptionStorage = new YamlTownCorruptionStorage(this);
            policyStorage = new YamlPolicyStorage(this);
            debugLogger.info("Initialized YAML Policy Storage");
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

        // Initialize policy manager
        policyManager = new PolicyManager(this, policyStorage);
        debugLogger.info("Initialized Policy Manager");

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

    /**
     * Loads and updates the config with new sections while preserving existing values
     */
    private void loadAndUpdateConfig() {
        // Create default config if it doesn't exist
        saveDefaultConfig();

        // Load the config
        FileConfiguration config = getConfig();

        // Set copyDefaults to true
        config.options().copyDefaults(true);

        // Add new default sections and values if they don't exist
        // Town Corruption Settings
        if (!config.contains("town_corruption")) {
            config.createSection("town_corruption");
            config.addDefault("town_corruption.base_daily_gain", 0.4);

            // Thresholds
            config.addDefault("town_corruption.thresholds.low", 25.0);
            config.addDefault("town_corruption.thresholds.medium", 50.0);
            config.addDefault("town_corruption.thresholds.high", 75.0);
            config.addDefault("town_corruption.thresholds.critical", 90.0);

            // Effects - taxation
            config.addDefault("town_corruption.effects.taxation.low", 0.95);
            config.addDefault("town_corruption.effects.taxation.medium", 0.90);
            config.addDefault("town_corruption.effects.taxation.high", 0.80);
            config.addDefault("town_corruption.effects.taxation.critical", 0.70);

            // Effects - trade
            config.addDefault("town_corruption.effects.trade.low", 0.95);
            config.addDefault("town_corruption.effects.trade.medium", 0.90);
            config.addDefault("town_corruption.effects.trade.high", 0.80);
            config.addDefault("town_corruption.effects.trade.critical", 0.70);
        }

        // Town Government Settings
        if (!config.contains("town_government")) {
            config.createSection("town_government");
            config.addDefault("town_government.change_cooldown", 15);
        }

        // Policy System Settings
        if (!config.contains("policies")) {
            config.createSection("policies");
            config.addDefault("policies.cooldown_days", 3);

            // Create custom policies section
            config.createSection("policies.available");
        }

        // Save updated config
        saveConfig();

        getLogger().info("Config loaded and updated with new settings");

        // Save policies.yml if it doesn't exist
        if (!new File(getDataFolder(), "policies.yml").exists()) {
            saveResource("policies.yml", false);
        }
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

        // Save policy data
        if (policyStorage != null) {
            policyStorage.saveAll();
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

        // Run the config update process again
        loadAndUpdateConfig();

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

        // Reload policy data
        if (policyManager != null) {
            policyManager.loadPolicies();
            policyManager.loadActivePolicies();
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