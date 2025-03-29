package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.ITownPoliticalPowerStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLTownPoliticalPowerStorage implements ITownPoliticalPowerStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;
    private final DelegateLogger logger;

    public MySQLTownPoliticalPowerStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
        this.logger = new DelegateLogger(plugin, "MySQLTownPPStorage");

        // Create table if it doesn't exist
        createTable();
        logger.info("MySQL Town Political Power Storage initialized");
    }

    private void createTable() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS " + prefix + "town_political_power (" +
                             "town_uuid VARCHAR(36) PRIMARY KEY, " +
                             "power_amount DOUBLE NOT NULL)")) {
            stmt.executeUpdate();
            logger.info("Town political power table created or verified");
        } catch (SQLException e) {
            logger.severe("Failed to create town political power table: " + e.getMessage());
        }
    }

    @Override
    public void savePP(UUID townUUID, double amount) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "town_political_power (town_uuid, power_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE power_amount = ?")) {

            stmt.setString(1, townUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            int updated = stmt.executeUpdate();
            logger.fine("Saved political power for town " + townUUID + ": " + amount +
                    " (rows affected: " + updated + ")");
        } catch (SQLException e) {
            logger.severe("Failed to save town political power: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllPP() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT town_uuid, power_amount FROM " + prefix + "town_political_power")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                    double pp = rs.getDouble("power_amount");
                    result.put(uuid, pp);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " town political power entries from database");
        } catch (SQLException e) {
            logger.severe("Failed to load town political power: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        logger.fine("saveAll() called (no action needed for MySQL storage)");
        // No specific action needed for MySQL as data is saved immediately
    }
}