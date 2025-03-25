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

public class YamlTownGovernmentStorage implements ITownGovernmentStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public YamlTownGovernmentStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "town_governments.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("towns");
                data.createSection("town_change_times");
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create town government data file: " + e.getMessage());
            }
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
            // Ensure sections exist
            if (!data.contains("towns")) {
                data.createSection("towns");
            }
            if (!data.contains("town_change_times")) {
                data.createSection("town_change_times");
            }
            try {
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not update town government data file: " + e.getMessage());
            }
        }
    }

    @Override
    public void saveGovernment(UUID uuid, GovernmentType type) {
        data.set("towns." + uuid.toString(), type.name());
        saveData();
    }

    @Override
    public void saveChangeTime(UUID uuid, long timestamp) {
        data.set("town_change_times." + uuid.toString(), timestamp);
        saveData();
    }

    @Override
    public GovernmentType getGovernment(UUID uuid) {
        String typeName = data.getString("towns." + uuid.toString());

        if (typeName == null) {
            return GovernmentType.AUTOCRACY; // Default
        }

        try {
            return GovernmentType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid town government type in storage: " + typeName);
            return GovernmentType.AUTOCRACY; // Default to AUTOCRACY if invalid
        }
    }

    @Override
    public long getChangeTime(UUID uuid) {
        return data.getLong("town_change_times." + uuid.toString(), 0);
    }

    @Override
    public Map<UUID, GovernmentType> loadAllGovernments() {
        Map<UUID, GovernmentType> result = new HashMap<>();

        if (data.isConfigurationSection("towns")) {
            for (String key : data.getConfigurationSection("towns").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String typeName = data.getString("towns." + key);
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    result.put(uuid, type);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in town government storage: " + key);
                }
            }
        }

        return result;
    }

    @Override
    public Map<UUID, Long> loadAllChangeTimes() {
        Map<UUID, Long> result = new HashMap<>();

        if (data.isConfigurationSection("town_change_times")) {
            for (String key : data.getConfigurationSection("town_change_times").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long time = data.getLong("town_change_times." + key);
                    result.put(uuid, time);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in town change time storage: " + key);
                }
            }
        }

        return result;
    }

    @Override
    public void saveAll() {
        saveData();
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save town government data: " + e.getMessage());
        }
    }
}