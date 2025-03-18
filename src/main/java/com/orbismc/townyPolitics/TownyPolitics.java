package com.orbismc.townyPolitics;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.command.commandobjects.CommandType;
import com.palmergames.bukkit.towny.command.commandobjects.AddonCommand;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townypolitics.commands.PoliticalPowerCommand;
import com.orbismc.townypolitics.listeners.TownyEventListener;
import com.orbismc.townypolitics.managers.PoliticalPowerManager;
import com.orbismc.townypolitics.storage.PoliticalPowerStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class TownyPolitics extends JavaPlugin {

    private TownyAPI townyAPI;
    private PoliticalPowerManager ppManager;
    private PoliticalPowerStorage storage;

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

        // Register events
        getServer().getPluginManager().registerEvents(new TownyEventListener(this, ppManager), this);

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

            // Create AddonCommand for the "politicalpower" subcommand
            AddonCommand politicalPowerCmd = new AddonCommand(CommandType.NATION, "politicalpower", ppCommand);

            // Add tab completion for nation names
            List<String> nationNames = townyAPI.getNations().stream()
                    .map(Nation::getName)
                    .collect(Collectors.toList());
            politicalPowerCmd.setTabCompletion(0, nationNames);

            // Register the command with Towny
            TownyCommandAddonAPI.addSubCommand(politicalPowerCmd);

            // Create and register the "pp" alias
            AddonCommand ppAlias = new AddonCommand(CommandType.NATION, "pp", ppCommand);
            ppAlias.setTabCompletion(0, nationNames);
            TownyCommandAddonAPI.addSubCommand(ppAlias);

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