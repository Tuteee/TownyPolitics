package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.IPoliticalPowerStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLPoliticalPowerStorage extends AbstractMySQLStorage implements IPoliticalPowerStorage {

    public MySQLPoliticalPowerStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLPPStorage");
        logger.info("MySQL Political Power Storage initialized");
    }

    @Override
    public void savePP(UUID nationUUID, double amount) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "political_power (nation_uuid, power_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE power_amount = ?")) {

            stmt.setString(1, nationUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
            logger.fine("Saved political power for nation " + nationUUID + ": " + amount);
        } catch (SQLException e) {
            logger.severe("Failed to save political power: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllPP() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT nation_uuid, power_amount FROM " + prefix + "political_power")) {

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("nation_uuid"));
                    double pp = rs.getDouble("power_amount");
                    result.put(uuid, pp);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " political power entries");
        } catch (SQLException e) {
            logger.severe("Failed to load political power: " + e.getMessage());
        }

        return result;
    }
}