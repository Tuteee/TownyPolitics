package com.orbismc.townyPolitics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.commands.GovernmentCommand;
import com.orbismc.townyPolitics.commands.OverviewCommand;
import com.orbismc.townyPolitics.commands.TownyAdminPoliticsCommand;
import com.orbismc.townyPolitics.commands.CorruptionCommand;
import com.orbismc.townyPolitics.commands.PoliticalPowerCommand;
import com.orbismc.townyPolitics.commands.TestEmbezzlementCommand;
import com.orbismc.townyPolitics.hooks.TransactionEmbezzlementHandler;
import com.orbismc.townyPolitics.hooks.DiagnosticTransactionHandler;
import com.orbismc.townyPolitics.listeners.TownyEventListener;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.TaxationManager;
import com.orbismc.townyPolitics.storage.GovernmentStorage;
import com.orbismc.townyPolitics.storage.PoliticalPowerStorage;
import com.orbismc.townyPolitics.storage.CorruptionStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyPolitics extends JavaPlugin {

    private TownyAPI townyAPI;
    private PoliticalPowerManager ppManager;
    private PoliticalPowerStorage ppStorage;
    private GovernmentManager govManager;
    private GovernmentStorage govStorage;
    private CorruptionManager corruptionManager;
    private CorruptionStorage corruptionStorage;
    private TaxationManager taxationManager;
    private TownyEventListener eventListener;

    @Override
    public void onEnable() {
        // Check if Towny is loaded
        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny not found! Disabling TownyPolitics...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        townyAPI = TownyAPI.getInstance();

        // Initialize storages
        ppStorage = new PoliticalPowerStorage(this);
        govStorage = new GovernmentStorage(this);
        corruptionStorage = new CorruptionStorage(this);

        // Initialize managers
        ppManager = new PoliticalPowerManager(this, ppStorage);
        govManager = new GovernmentManager(this, govStorage);

        // Initialize corruption manager (needs govManager)
        corruptionManager = new CorruptionManager(this, corruptionStorage, govManager);

        // Initialize taxation manager (depends on corruption manager)
        taxationManager = new TaxationManager(this, corruptionManager);

        // Initialize and register listener
        eventListener = new TownyEventListener(this, ppManager, corruptionManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Register improved transaction embezzlement handler
        TransactionEmbezzlementHandler embezzlementHandler = new TransactionEmbezzlementHandler(this);
        getServer().getPluginManager().registerEvents(embezzlementHandler, this);
        getLogger().info("Registered Improved Transaction Embezzlement Handler");

        // Keep the diagnostic handler if you want to see events for debugging
        DiagnosticTransactionHandler diagnosticHandler = new DiagnosticTransactionHandler(this);
        getServer().getPluginManager().registerEvents(diagnosticHandler, this);
        getLogger().info("Registered Diagnostic Transaction Handler");

        // Connect listener and manager (circular reference)
        ppManager.setEventListener(eventListener);

        // Register commands
        registerCommands();

        // Save default config
        saveDefaultConfig();

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
        if (corruptionStorage != null) {
            corruptionStorage.saveAll();
        }
        getLogger().info("TownyPolitics has been disabled!");
    }

    public void reload() {
        reloadConfig();
        if (ppManager != null) {
            ppManager.loadData();
        }
        if (govManager != null) {
            govManager.loadData();
        }
        if (corruptionManager != null) {
            corruptionManager.loadData();
        }
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        try {
            // Create command executors
            GovernmentCommand townGovCommand = new GovernmentCommand(this, govManager, "town");
            GovernmentCommand nationGovCommand = new GovernmentCommand(this, govManager, "nation");
            OverviewCommand overviewCommand = new OverviewCommand(this, govManager, ppManager, corruptionManager);
            CorruptionCommand corruptionCommand = new CorruptionCommand(this, corruptionManager, ppManager);
            PoliticalPowerCommand ppCommand = new PoliticalPowerCommand(this, ppManager);

            // Register town commands
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "government", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "gov", townGovCommand);

            // Register nation commands
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "government", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "gov", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "overview", overviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "o", overviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "corruption", corruptionCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "pp", ppCommand);

            // Register TownyAdmin command
            new TownyAdminPoliticsCommand(this, govManager, ppManager, corruptionManager);

            // Register test command for embezzlement
            TestEmbezzlementCommand testEmbezzlementCommand = new TestEmbezzlementCommand(this);
            this.getCommand("taxtest").setExecutor(testEmbezzlementCommand);
            getLogger().info("Registered tax testing command");

            getLogger().info("Successfully registered all commands.");
        } catch (Exception e) {
            getLogger().severe("Failed to register commands: " + e.getMessage());
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
        return corruptionManager;
    }

    public TaxationManager getTaxationManager() {
        return taxationManager;
    }
}