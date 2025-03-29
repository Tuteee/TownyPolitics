package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.TownyPolitics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlTownPoliticalPowerStorage implements ITownPoliticalPowerStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public YamlTownPoliticalPowerStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    /**
     * Set up the storage file
     */
    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "town_political_power.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("towns");
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create town political power data file: " + e.getMessage());
            }
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
            if (!data.contains("towns")) {
                data.createSection("towns");
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not update town political power data file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Save political power for a town
     *
     * @param townUUID The town UUID
     * @param amount The amount of PP
     */
    @Override
    public void savePP(UUID townUUID, double amount) {
        data.set("towns." + townUUID.toString(), amount);
        saveData();
    }

    /**
     * Load all political power data
     *
     * @return Map of town UUIDs to their political power
     */
    @Override
    public Map<UUID, Double> loadAllPP() {
        Map<UUID, Double> result = new HashMap<>();

        if (data.contains("towns")) {
            if (data.isConfigurationSection("towns")) {
                for (String key : data.getConfigurationSection("towns").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        double pp = data.getDouble("towns." + key);
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
            plugin.getLogger().severe("Could not save town political power data: " + e.getMessage());
        }
    }
}