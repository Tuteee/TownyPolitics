package com.orbismc.townyPolitics.commands;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.YamlCorruptionStorage;
import com.orbismc.townyPolitics.storage.YamlGovernmentStorage;
import com.orbismc.townyPolitics.storage.YamlPoliticalPowerStorage;
import com.orbismc.townyPolitics.storage.YamlTownPoliticalPowerStorage;
import com.orbismc.townyPolitics.storage.mysql.MySQLCorruptionStorage;
import com.orbismc.townyPolitics.storage.mysql.MySQLGovernmentStorage;
import com.orbismc.townyPolitics.storage.mysql.MySQLPoliticalPowerStorage;
import com.orbismc.townyPolitics.storage.mysql.MySQLTownPoliticalPowerStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MigrationCommand implements CommandExecutor {

    private final TownyPolitics plugin;

    public MigrationCommand(TownyPolitics plugin) {
        this.plugin = plugin;
        plugin.getCommand("townypoliticsmigrate").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("townypolitics.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting data migration from YAML to MySQL...");

        try {
            // Load YAML data
            YamlPoliticalPowerStorage yamlPP = new YamlPoliticalPowerStorage(plugin);
            YamlGovernmentStorage yamlGov = new YamlGovernmentStorage(plugin);
            YamlCorruptionStorage yamlCorruption = new YamlCorruptionStorage(plugin);
            YamlTownPoliticalPowerStorage yamlTownPP = new YamlTownPoliticalPowerStorage(plugin);

            // Get MySQL storage
            MySQLPoliticalPowerStorage mysqlPP = new MySQLPoliticalPowerStorage(plugin, plugin.getDatabaseManager());
            MySQLGovernmentStorage mysqlGov = new MySQLGovernmentStorage(plugin, plugin.getDatabaseManager());
            MySQLCorruptionStorage mysqlCorruption = new MySQLCorruptionStorage(plugin, plugin.getDatabaseManager());
            MySQLTownPoliticalPowerStorage mysqlTownPP = new MySQLTownPoliticalPowerStorage(plugin, plugin.getDatabaseManager());

            // Migrate Political Power
            yamlPP.loadAllPP().forEach(mysqlPP::savePP);
            sender.sendMessage(ChatColor.GREEN + "Migrated Nation Political Power data");

            // Migrate Town Political Power
            yamlTownPP.loadAllPP().forEach(mysqlTownPP::savePP);
            sender.sendMessage(ChatColor.GREEN + "Migrated Town Political Power data");

            // Migrate Government data
            yamlGov.loadAllGovernments(true).forEach((uuid, type) ->
                    mysqlGov.saveGovernment(uuid, type, true));
            yamlGov.loadAllGovernments(false).forEach((uuid, type) ->
                    mysqlGov.saveGovernment(uuid, type, false));

            yamlGov.loadAllChangeTimes(true).forEach((uuid, time) ->
                    mysqlGov.saveChangeTime(uuid, time, true));
            yamlGov.loadAllChangeTimes(false).forEach((uuid, time) ->
                    mysqlGov.saveChangeTime(uuid, time, false));
            sender.sendMessage(ChatColor.GREEN + "Migrated Government data");

            // Migrate Corruption data
            yamlCorruption.loadAllCorruption(true).forEach((uuid, amount) ->
                    mysqlCorruption.saveCorruption(uuid, amount, true));
            sender.sendMessage(ChatColor.GREEN + "Migrated Corruption data");

            sender.sendMessage(ChatColor.GREEN + "Data migration completed successfully!");
            sender.sendMessage(ChatColor.YELLOW + "Set 'database.use_mysql' to true in config.yml and restart the server to use MySQL.");

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Migration failed: " + e.getMessage());
            plugin.getLogger().severe("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}