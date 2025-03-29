package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.ITownPoliticalPowerStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLTownPoliticalPowerStorage extends AbstractMySQLStorage implements ITownPoliticalPowerStorage {

    public MySQLTownPoliticalPowerStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLTownPPStorage");
        logger.info("MySQL Town Political Power Storage initialized");
    }

    @Override
    public void savePP(UUID townUUID, double amount) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "town_political_power (town_uuid, power_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE power_amount = ?")) {

            stmt.setString(1, townUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
            logger.fine("Saved political power for town " + townUUID + ": " + amount);
        } catch (SQLException e) {
            logger.severe("Failed to save town political power: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllPP() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = getConnection();
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

            logger.info("Loaded " + count + " town political power entries");
        } catch (SQLException e) {
            logger.severe("Failed to load town political power: " + e.getMessage());
        }

        return result;
    }
}