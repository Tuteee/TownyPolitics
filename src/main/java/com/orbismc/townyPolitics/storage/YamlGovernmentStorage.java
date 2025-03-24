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

public class YamlGovernmentStorage implements IGovernmentStorage {

    private final TownyPolitics plugin;
    private File dataFile;
    private FileConfiguration data;

    public YamlGovernmentStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        setupStorage();
    }

    private void setupStorage() {
        dataFile = new File(plugin.getDataFolder(), "governments.yml");

        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
                data = YamlConfiguration.loadConfiguration(dataFile);
                data.createSection("towns");
                data.createSection("nations");
                data.createSection("town_change_times");
                data.createSection("nation_change_times");
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
            if (!data.contains("town_change_times")) {
                data.createSection("town_change_times");
            }
            if (!data.contains("nation_change_times")) {
                data.createSection("nation_change_times");
            }
            try {
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not update government data file: " + e.getMessage());
            }
        }
    }

    @Override
    public void saveGovernment(UUID uuid, GovernmentType type, boolean isNation) {
        String section = isNation ? "nations" : "towns";
        data.set(section + "." + uuid.toString(), type.name());
        saveData();
    }

    @Override
    public void saveChangeTime(UUID uuid, long timestamp, boolean isNation) {
        String section = isNation ? "nation_change_times" : "town_change_times";
        data.set(section + "." + uuid.toString(), timestamp);
        saveData();
    }

    @Override
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

    @Override
    public long getChangeTime(UUID uuid, boolean isNation) {
        String section = isNation ? "nation_change_times" : "town_change_times";
        return data.getLong(section + "." + uuid.toString(), 0);
    }

    @Override
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

    @Override
    public Map<UUID, Long> loadAllChangeTimes(boolean isNation) {
        Map<UUID, Long> result = new HashMap<>();
        String section = isNation ? "nation_change_times" : "town_change_times";

        if (data.isConfigurationSection(section)) {
            for (String key : data.getConfigurationSection(section).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long time = data.getLong(section + "." + key);
                    result.put(uuid, time);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in change time storage: " + key);
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
            plugin.getLogger().severe("Could not save government data: " + e.getMessage());
        }
    }
}