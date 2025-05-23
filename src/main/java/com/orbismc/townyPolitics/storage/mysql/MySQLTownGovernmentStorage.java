package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.ITownGovernmentStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLTownGovernmentStorage extends AbstractMySQLStorage implements ITownGovernmentStorage {

    public MySQLTownGovernmentStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLTownGovStorage");
        logger.info("MySQL Town Government Storage initialized");
    }

    @Override
    public void saveGovernment(UUID uuid, GovernmentType type) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "governments (entity_uuid, entity_type, government_type, last_change_time) " +
                             "VALUES (?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE government_type = ?, last_change_time = ?")) {

            long now = System.currentTimeMillis();

            stmt.setString(1, uuid.toString());
            stmt.setString(2, "TOWN");
            stmt.setString(3, type.name());
            stmt.setLong(4, now);
            stmt.setString(5, type.name());
            stmt.setLong(6, now);

            int updated = stmt.executeUpdate();
            logger.fine("Saved government type for town " + uuid + ": " + type.name());
        } catch (SQLException e) {
            logger.severe("Failed to save town government data: " + e.getMessage());
        }
    }

    @Override
    public void saveChangeTime(UUID uuid, long timestamp) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE " + prefix + "governments SET last_change_time = ? " +
                             "WHERE entity_uuid = ? AND entity_type = 'TOWN'")) {

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
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT government_type FROM " + prefix + "governments " +
                             "WHERE entity_uuid = ? AND entity_type = 'TOWN'")) {

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String typeName = rs.getString("government_type");
                try {
                    return GovernmentType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid government type in database: " + typeName);
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get town government: " + e.getMessage());
        }

        return GovernmentType.AUTOCRACY; // Default
    }

    @Override
    public long getChangeTime(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT last_change_time FROM " + prefix + "governments " +
                             "WHERE entity_uuid = ? AND entity_type = 'TOWN'")) {

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_change_time");
            }
        } catch (SQLException e) {
            logger.severe("Failed to get town change time: " + e.getMessage());
        }

        return 0L; // Default
    }

    @Override
    public Map<UUID, GovernmentType> loadAllGovernments() {
        Map<UUID, GovernmentType> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, government_type FROM " + prefix + "governments " +
                             "WHERE entity_type = 'TOWN'")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    String typeName = rs.getString("government_type");
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    result.put(uuid, type);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in town government database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " town government entries");
        } catch (SQLException e) {
            logger.severe("Failed to load town governments: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<UUID, Long> loadAllChangeTimes() {
        Map<UUID, Long> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, last_change_time FROM " + prefix + "governments " +
                             "WHERE entity_type = 'TOWN'")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    long time = rs.getLong("last_change_time");
                    result.put(uuid, time);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in town government database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " town change time entries");
        } catch (SQLException e) {
            logger.severe("Failed to load town change times: " + e.getMessage());
        }

        return result;
    }
}