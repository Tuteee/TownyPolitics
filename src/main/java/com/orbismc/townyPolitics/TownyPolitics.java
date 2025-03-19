package com.orbismc.townyPolitics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.commands.PoliticalPowerCommand;
import com.orbismc.townyPolitics.listeners.TownyEventListener;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.orbismc.townyPolitics.storage.PoliticalPowerStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyPolitics extends JavaPlugin {

    private TownyAPI townyAPI;
    private PoliticalPowerManager ppManager;
    private PoliticalPowerStorage storage;
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

        // Initialize storage
        storage = new PoliticalPowerStorage(this);

        // Initialize manager
        ppManager = new PoliticalPowerManager(this, storage);

        // Initialize and register listener
        eventListener = new TownyEventListener(this, ppManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Connect listener and manager (circular reference)
        ppManager.setEventListener(eventListener);

        // Register nation command
        registerNationCommand();

        // Save default config
        saveDefaultConfig();

        getLogger().info("TownyPolitics has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data on plugin disable
        if (storage != null) {
            storage.saveAll();
        }
        getLogger().info("TownyPolitics has been disabled!");
    }

    /**
     * Register the politicalpower subcommand for the nation command
     */
    private void registerNationCommand() {
        try {
            // Create command executor
            PoliticalPowerCommand ppCommand = new PoliticalPowerCommand(this, ppManager);

            // Register the command with Towny using CommandType.NATION
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "politicalpower", ppCommand);

            // Register an alias for shorter typing
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "pp", ppCommand);

            getLogger().info("Successfully registered the 'politicalpower' and 'pp' subcommands for nation command.");
        } catch (Exception e) {
            getLogger().severe("Failed to register nation subcommand: " + e.getMessage());
        }
    }

    public TownyAPI getTownyAPI() {
        return townyAPI;
    }

    public PoliticalPowerManager getPPManager() {
        return ppManager;
    }
}