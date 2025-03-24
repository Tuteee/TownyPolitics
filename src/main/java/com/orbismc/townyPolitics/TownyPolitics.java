package com.orbismc.townyPolitics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.commands.GovernmentCommand;
import com.orbismc.townyPolitics.commands.OverviewCommand;
import com.orbismc.townyPolitics.commands.TownyAdminPoliticsCommand;
import com.orbismc.townyPolitics.hooks.TownyTaxTransactionHandler;
import com.orbismc.townyPolitics.hooks.TownyNationTaxHandler;
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

        // Register Towny hooks
        TownyTaxTransactionHandler taxHandler = new TownyTaxTransactionHandler(this);
        getServer().getPluginManager().registerEvents(taxHandler, this);
        getLogger().info("Registered Towny taxation hooks.");

        // Register Nation Tax Handler
        TownyNationTaxHandler nationTaxHandler = new TownyNationTaxHandler(this);
        getServer().getPluginManager().registerEvents(nationTaxHandler, this);
        getLogger().info("Registered Towny nation taxation hooks.");

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

            // Register town commands
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "government", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "gov", townGovCommand);

            // Register nation commands
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "government", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "gov", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "overview", overviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "o", overviewCommand);

            // Register TownyAdmin command
            new TownyAdminPoliticsCommand(this, govManager, ppManager, corruptionManager);

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