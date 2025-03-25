// MySQLTownGovernmentStorage.java
package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.ITownGovernmentStorage;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLTownGovernmentStorage implements ITownGovernmentStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;
    private final DelegateLogger logger;

    public MySQLTownGovernmentStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
        this.logger = new DelegateLogger(plugin, "MySQLTownGovStorage");

        // Create table if it doesn't exist
        createTable();
        logger.info("MySQL Town Government Storage initialized");
    }

    private void createTable() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS " + prefix + "town_governments (" +
                             "town_uuid VARCHAR(36) PRIMARY KEY, " +
                             "government_type VARCHAR(50) NOT NULL, " +
                             "last_change_time BIGINT NOT NULL)")) {
            stmt.executeUpdate();
            logger.info("Town governments table created or verified");
        } catch (SQLException e) {
            logger.severe("Failed to create town governments table: " + e.getMessage());
        }
    }

    @Override
    public void saveGovernment(UUID uuid, GovernmentType type) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "town_governments (town_uuid, government_type, last_change_time) " +
                             "VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE government_type = ?, last_change_time = ?")) {

            long now = System.currentTimeMillis();

            stmt.setString(1, uuid.toString());
            stmt.setString(2, type.name());
            stmt.setLong(3, now);
            stmt.setString(4, type.name());
            stmt.setLong(5, now);

            int updated = stmt.executeUpdate();
            logger.fine("Saved government type for town " + uuid + ": " + type.name() + " (rows affected: " + updated + ")");
        } catch (SQLException e) {
            logger.severe("Failed to save town government data: " + e.getMessage());
        }
    }

    @Override
    public void saveChangeTime(UUID uuid, long timestamp) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE " + prefix + "town_governments SET last_change_time = ? " +
                             "WHERE town_uuid = ?")) {

            stmt.setLong(1, timestamp);
            stmt.setString(2, uuid.toString());

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.fine("Updated change time for town " + uuid + " to " + timestamp);
            } else {
                logger.warning("Failed to update change time for town " + uuid + " - not found in database");
            }
        } catch (SQLException e) {
            logger.severe("Failed to save town government change time: " + e.getMessage());
        }
    }

    @Override
    public GovernmentType getGovernment(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT government_type FROM " + prefix + "town_governments " +
                             "WHERE town_uuid = ?")) {

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String typeName = rs.getString("government_type");
                try {
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    logger.fine("Retrieved government type for town " + uuid + ": " + type.name());
                    return type;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid government type in database: " + typeName);
                }
            } else {
                logger.fine("No government type found for town " + uuid + ", using default");
            }
        } catch (SQLException e) {
            logger.severe("Failed to get town government: " + e.getMessage());
        }

        return GovernmentType.AUTOCRACY; // Default
    }

    @Override
    public long getChangeTime(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT last_change_time FROM " + prefix + "town_governments " +
                             "WHERE town_uuid = ?")) {

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long changeTime = rs.getLong("last_change_time");
                logger.fine("Retrieved change time for town " + uuid + ": " + changeTime);
                return changeTime;
            }
        } catch (SQLException e) {
            logger.severe("Failed to get town change time: " + e.getMessage());
        }

        return 0L; // Default
    }

    @Override
    public Map<UUID, GovernmentType> loadAllGovernments() {
        Map<UUID, GovernmentType> result = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT town_uuid, government_type FROM " + prefix + "town_governments")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                    String typeName = rs.getString("government_type");
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    result.put(uuid, type);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in town government database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " town government entries from database");
        } catch (SQLException e) {
            logger.severe("Failed to load town governments: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<UUID, Long> loadAllChangeTimes() {
        Map<UUID, Long> result = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT town_uuid, last_change_time FROM " + prefix + "town_governments")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                    long time = rs.getLong("last_change_time");
                    result.put(uuid, time);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in town government database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " town change time entries from database");
        } catch (SQLException e) {
            logger.severe("Failed to load town change times: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        logger.fine("saveAll() called (no action needed for MySQL storage)");
        // No specific action needed for MySQL as data is saved immediately
    }
}