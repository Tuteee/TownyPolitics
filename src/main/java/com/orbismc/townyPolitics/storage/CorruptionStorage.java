package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.TownyPolitics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorruptionStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public CorruptionStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    /**
     * Set up the storage file
     */
    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "corruption.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("nations");
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create corruption data file: " + e.getMessage());
            }
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
            if (!data.contains("nations")) {
                data.createSection("nations");
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not update corruption data file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Save corruption level for a nation or town
     *
     * @param uuid The nation or town UUID
     * @param amount The amount of corruption
     * @param isNation Whether this is a nation (true) or town (false)
     */
    public void saveCorruption(UUID uuid, double amount, boolean isNation) {
        String section = isNation ? "nations" : "towns";
        data.set(section + "." + uuid.toString(), amount);
        saveData();
    }

    /**
     * Load all corruption data for nations or towns
     *
     * @param isNation Whether to load nations (true) or towns (false)
     * @return Map of UUIDs to their corruption levels
     */
    public Map<UUID, Double> loadAllCorruption(boolean isNation) {
        Map<UUID, Double> result = new HashMap<>();
        String section = isNation ? "nations" : "towns";

        if (data.contains(section)) {
            if (data.isConfigurationSection(section)) {
                for (String key : data.getConfigurationSection(section).getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        double corruption = data.getDouble(section + "." + key);
                        result.put(uuid, corruption);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in corruption data storage: " + key);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Save all data to file
     */
    public void saveAll() {
        saveData();
    }

    /**
     * Save data to file
     */
    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save corruption data: " + e.getMessage());
        }
    }
}