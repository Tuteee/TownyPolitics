package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.ICorruptionStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLCorruptionStorage extends AbstractMySQLStorage implements ICorruptionStorage {

    public MySQLCorruptionStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLCorruptionStorage");
        logger.info("MySQL Corruption Storage initialized");
    }

    @Override
    public void saveCorruption(UUID uuid, double amount, boolean isNation) {
        String tableName = isNation ? "corruption" : "town_corruption";
        String columnName = isNation ? "nation_uuid" : "town_uuid";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + tableName + " (" + columnName + ", corruption_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE corruption_amount = ?")) {

            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
            logger.fine("Saved corruption for " + (isNation ? "nation" : "town") + " " + uuid + ": " + amount);
        } catch (SQLException e) {
            logger.severe("Failed to save corruption: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllCorruption(boolean isNation) {
        Map<UUID, Double> result = new HashMap<>();
        String tableName = isNation ? "corruption" : "town_corruption";
        String columnName = isNation ? "nation_uuid" : "town_uuid";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT " + columnName + ", corruption_amount FROM " + prefix + tableName)) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString(columnName));
                    double corruption = rs.getDouble("corruption_amount");
                    result.put(uuid, corruption);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " corruption entries for " +
                    (isNation ? "nations" : "towns"));
        } catch (SQLException e) {
            logger.severe("Failed to load corruption: " + e.getMessage());
        }

        return result;
    }
}