package com.orbismc.townyPolitics;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final TownyPolitics plugin;
    private FileConfiguration config;
    private FileConfiguration policiesConfig;
    private FileConfiguration townPoliciesConfig;

    public ConfigManager(TownyPolitics plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        ensureConfigDefaults();
        plugin.saveConfig();

        loadAdditionalConfigs();
    }

    private void ensureConfigDefaults() {
        ensureTownPowerDefaults();
        ensureTownCorruptionDefaults();
        ensureTownGovernmentDefaults();
        ensurePolicyDefaults();
        ensureDatabaseDefaults();
    }

    private void ensureTownPowerDefaults() {
        if (!config.contains("town_political_power")) {
            config.createSection("town_political_power");
            config.addDefault("town_political_power.base_gain", 0.5);
            config.addDefault("town_political_power.max_daily_gain", 3.0);
            config.addDefault("town_political_power.min_daily_gain", 0.5);
            config.addDefault("town_political_power.nation_bonus", 0.1);
        }
    }

    private void ensureTownCorruptionDefaults() {
        if (!config.contains("town_corruption")) {
            config.createSection("town_corruption");
            config.addDefault("town_corruption.base_daily_gain", 0.4);

            config.addDefault("town_corruption.thresholds.low", 25.0);
            config.addDefault("town_corruption.thresholds.medium", 50.0);
            config.addDefault("town_corruption.thresholds.high", 75.0);
            config.addDefault("town_corruption.thresholds.critical", 90.0);

            config.addDefault("town_corruption.effects.taxation.low", 0.95);
            config.addDefault("town_corruption.effects.taxation.medium", 0.90);
            config.addDefault("town_corruption.effects.taxation.high", 0.80);
            config.addDefault("town_corruption.effects.taxation.critical", 0.70);

            config.addDefault("town_corruption.effects.trade.low", 0.95);
            config.addDefault("town_corruption.effects.trade.medium", 0.90);
            config.addDefault("town_corruption.effects.trade.high", 0.80);
            config.addDefault("town_corruption.effects.trade.critical", 0.70);
        }
    }

    private void ensureTownGovernmentDefaults() {
        if (!config.contains("town_government")) {
            config.createSection("town_government");
            config.addDefault("town_government.change_cooldown", 15);
        }
    }

    private void ensurePolicyDefaults() {
        if (!config.contains("policies")) {
            config.createSection("policies");
            config.addDefault("policies.cooldown_days", 3);
        }
    }

    private void ensureDatabaseDefaults() {
        if (!config.contains("database")) {
            config.createSection("database");
            config.addDefault("database.host", "localhost");
            config.addDefault("database.port", 3306);
            config.addDefault("database.database", "townypolitics");
            config.addDefault("database.username", "root");
            config.addDefault("database.password", "password");
            config.addDefault("database.prefix", "tp_");
            config.addDefault("database.connection_pool_size", 10);
            config.addDefault("database.max_lifetime", 1800000);
        }
    }

    private void loadAdditionalConfigs() {
        File policiesFile = new File(plugin.getDataFolder(), "policies.yml");
        if (!policiesFile.exists()) {
            plugin.saveResource("policies.yml", false);
        }
        policiesConfig = YamlConfiguration.loadConfiguration(policiesFile);

        File townPoliciesFile = new File(plugin.getDataFolder(), "town_policies.yml");
        if (!townPoliciesFile.exists()) {
            plugin.saveResource("town_policies.yml", false);
        }
        townPoliciesConfig = YamlConfiguration.loadConfiguration(townPoliciesFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getPoliciesConfig() {
        return policiesConfig;
    }

    public FileConfiguration getTownPoliciesConfig() {
        return townPoliciesConfig;
    }

    public void saveConfigs() {
        plugin.saveConfig();
    }
}