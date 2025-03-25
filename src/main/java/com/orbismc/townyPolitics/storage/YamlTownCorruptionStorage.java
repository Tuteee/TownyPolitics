package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.TownyPolitics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlTownCorruptionStorage implements ITownCorruptionStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public YamlTownCorruptionStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    /**
     * Set up the storage file
     */
    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "town_corruption.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("towns");
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create town corruption data file: " + e.getMessage());
            }
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
            if (!data.contains("towns")) {
                data.createSection("towns");
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not update town corruption data file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Save corruption level for a town
     *
     * @param uuid The town UUID
     * @param amount The amount of corruption
     */
    @Override
    public void saveCorruption(UUID uuid, double amount) {
        data.set("towns." + uuid.toString(), amount);
        saveData();
    }

    /**
     * Load all corruption data for towns
     *
     * @return Map of UUIDs to their corruption levels
     */
    @Override
    public Map<UUID, Double> loadAllCorruption() {
        Map<UUID, Double> result = new HashMap<>();

        if (data.contains("towns")) {
            if (data.isConfigurationSection("towns")) {
                for (String key : data.getConfigurationSection("towns").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        double corruption = data.getDouble("towns." + key);
                        result.put(uuid, corruption);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in town corruption data storage: " + key);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Save all data to file
     */
    @Override
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
            plugin.getLogger().severe("Could not save town corruption data: " + e.getMessage());
        }
    }
}