package com.orbismc.townyPolitics.storage;

import com.orbismc.townypolitics.TownyPolitics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoliticalPowerStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public PoliticalPowerStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    /**
     * Set up the storage file
     */
    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "political_power.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("nations");
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create political power data file: " + e.getMessage());
            }
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
            if (!data.contains("nations")) {
                data.createSection("nations");
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not update political power data file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Save political power for a nation
     *
     * @param nationUUID The nation UUID
     * @param amount The amount of PP
     */
    public void savePP(UUID nationUUID, double amount) {
        data.set("nations." + nationUUID.toString(), amount);
        saveData();
    }

    /**
     * Load all political power data
     *
     * @return Map of nation UUIDs to their political power
     */
    public Map<UUID, Double> loadAllPP() {
        Map<UUID, Double> result = new HashMap<>();

        if (data.contains("nations")) {
            if (data.isConfigurationSection("nations")) {
                for (String key : data.getConfigurationSection("nations").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        double pp = data.getDouble("nations." + key);
                        result.put(uuid, pp);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in data storage: " + key);
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
            plugin.getLogger().severe("Could not save political power data: " + e.getMessage());
        }
    }
}