package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.IGovernmentStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLGovernmentStorage implements IGovernmentStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;

    public MySQLGovernmentStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
    }

    @Override
    public void saveGovernment(UUID uuid, GovernmentType type, boolean isNation) {
        String entityType = isNation ? "NATION" : "TOWN";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "governments (entity_uuid, entity_type, government_type, last_change_time) " +
                             "VALUES (?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE government_type = ?, last_change_time = ?")) {

            long now = System.currentTimeMillis();

            stmt.setString(1, uuid.toString());
            stmt.setString(2, entityType);
            stmt.setString(3, type.name());
            stmt.setLong(4, now);
            stmt.setString(5, type.name());
            stmt.setLong(6, now);

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save government data: " + e.getMessage());
        }
    }

    @Override
    public void saveChangeTime(UUID uuid, long timestamp, boolean isNation) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE " + prefix + "governments SET last_change_time = ? " +
                             "WHERE entity_uuid = ?")) {

            stmt.setLong(1, timestamp);
            stmt.setString(2, uuid.toString());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save government change time: " + e.getMessage());
        }
    }

    @Override
    public GovernmentType getGovernment(UUID uuid, boolean isNation) {
        String entityType = isNation ? "NATION" : "TOWN";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT government_type FROM " + prefix + "governments " +
                             "WHERE entity_uuid = ? AND entity_type = ?")) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, entityType);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String typeName = rs.getString("government_type");
                try {
                    return GovernmentType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid government type in database: " + typeName);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get government: " + e.getMessage());
        }

        return GovernmentType.AUTOCRACY; // Default
    }

    @Override
    public long getChangeTime(UUID uuid, boolean isNation) {
        String entityType = isNation ? "NATION" : "TOWN";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT last_change_time FROM " + prefix + "governments " +
                             "WHERE entity_uuid = ? AND entity_type = ?")) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, entityType);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_change_time");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get change time: " + e.getMessage());
        }

        return 0L; // Default
    }

    @Override
    public Map<UUID, GovernmentType> loadAllGovernments(boolean isNation) {
        Map<UUID, GovernmentType> result = new HashMap<>();
        String entityType = isNation ? "NATION" : "TOWN";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, government_type FROM " + prefix + "governments " +
                             "WHERE entity_type = ?")) {

            stmt.setString(1, entityType);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    String typeName = rs.getString("government_type");
                    GovernmentType type = GovernmentType.valueOf(typeName);
                    result.put(uuid, type);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load governments: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<UUID, Long> loadAllChangeTimes(boolean isNation) {
        Map<UUID, Long> result = new HashMap<>();
        String entityType = isNation ? "NATION" : "TOWN";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, last_change_time FROM " + prefix + "governments " +
                             "WHERE entity_type = ?")) {

            stmt.setString(1, entityType);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    long time = rs.getLong("last_change_time");
                    result.put(uuid, time);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data in database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load change times: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        // No specific action needed for MySQL as data is saved immediately
    }
}