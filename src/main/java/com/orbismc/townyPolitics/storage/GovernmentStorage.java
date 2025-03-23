package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GovernmentStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public GovernmentStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    /**
     * Set up the storage file
     */
    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "governments.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("towns");
                data.createSection("nations");
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create government data file: " + e.getMessage());
            }
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
            // Ensure sections exist
            if (!data.contains("towns")) {
                data.createSection("towns");
            }
            if (!data.contains("nations")) {
                data.createSection("nations");
            }
            try {
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not update government data file: " + e.getMessage());
            }
        }
    }

    /**
     * Save government type for a town or nation
     *
     * @param uuid The town or nation UUID
     * @param type The government type
     * @param isNation True if this is for a nation, false for town
     */
    public void saveGovernment(UUID uuid, GovernmentType type, boolean isNation) {
        String section = isNation ? "nations" : "towns";
        data.set(section + "." + uuid.toString(), type.name());
        saveData();
    }

    /**
     * Get government type for a town or nation
     *
     * @param uuid The town or nation UUID
     * @param isNation True if this is for a nation, false for town
     * @return The government type, AUTOCRACY by default
     */
    public GovernmentType getGovernment(UUID uuid, boolean isNation) {
        String section = isNation ? "nations" : "towns";
        String typeName = data.getString(section + "." + uuid.toString());

        if (typeName == null) {
            return GovernmentType.AUTOCRACY; // Default
        }

        try {
            return GovernmentType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid government type in storage: " + typeName);
            return GovernmentType.AUTOCRACY; // Default to AUTOCRACY if invalid
        }
    }

    /**
     * Load all government data
     *
     * @param isNation True to load nations, false for towns
     * @return Map of UUIDs to their government types
     */
    public Map<UUID, GovernmentType> loadAllGovernments(boolean isNation) {
        Map<UUID, GovernmentType> result = new HashMap<>();
        String section = isNation ? "nations" : "towns";

        if (data.isConfigurationSection(section)) {
            for (String key : data.getConfigurationSection(section).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String typeName = data.getString(section + "." + key);
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    result.put(uuid, type);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in government storage: " + key);
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
            plugin.getLogger().severe("Could not save government data: " + e.getMessage());
        }
    }
}