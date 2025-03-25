// MySQLTownCorruptionStorage.java
package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.ITownCorruptionStorage;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLTownCorruptionStorage implements ITownCorruptionStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;
    private final DelegateLogger logger;

    public MySQLTownCorruptionStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
        this.logger = new DelegateLogger(plugin, "MySQLTownCorruptionStorage");

        // Create table if it doesn't exist
        createTable();
        logger.info("MySQL Town Corruption Storage initialized");
    }

    private void createTable() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS " + prefix + "town_corruption (" +
                             "town_uuid VARCHAR(36) PRIMARY KEY, " +
                             "corruption_amount DOUBLE NOT NULL)")) {
            stmt.executeUpdate();
            logger.info("Town corruption table created or verified");
        } catch (SQLException e) {
            logger.severe("Failed to create town corruption table: " + e.getMessage());
        }
    }

    @Override
    public void saveCorruption(UUID uuid, double amount) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "town_corruption (town_uuid, corruption_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE corruption_amount = ?")) {

            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            int updated = stmt.executeUpdate();
            logger.fine("Saved corruption for town " + uuid + ": " + amount + " (rows affected: " + updated + ")");
        } catch (SQLException e) {
            logger.severe("Failed to save town corruption: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllCorruption() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT town_uuid, corruption_amount FROM " + prefix + "town_corruption")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                    double corruption = rs.getDouble("corruption_amount");
                    result.put(uuid, corruption);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in town corruption database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " town corruption entries from database");
        } catch (SQLException e) {
            logger.severe("Failed to load town corruption: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        logger.fine("saveAll() called (no action needed for MySQL storage)");
        // No specific action needed for MySQL as data is saved immediately
    }
}