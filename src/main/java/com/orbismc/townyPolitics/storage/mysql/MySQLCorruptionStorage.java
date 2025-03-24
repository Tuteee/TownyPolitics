package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.ICorruptionStorage;
import com.orbismc.townyPolitics.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLCorruptionStorage implements ICorruptionStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;

    public MySQLCorruptionStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
    }

    @Override
    public void saveCorruption(UUID uuid, double amount, boolean isNation) {
        if (!isNation) {
            // Currently only support corruption for nations
            return;
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "corruption (nation_uuid, corruption_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE corruption_amount = ?")) {

            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save corruption: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllCorruption(boolean isNation) {
        Map<UUID, Double> result = new HashMap<>();

        if (!isNation) {
            // Currently only support corruption for nations
            return result;
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT nation_uuid, corruption_amount FROM " + prefix + "corruption")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("nation_uuid"));
                    double corruption = rs.getDouble("corruption_amount");
                    result.put(uuid, corruption);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load corruption: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        // No specific action needed for MySQL as data is saved immediately
    }
}