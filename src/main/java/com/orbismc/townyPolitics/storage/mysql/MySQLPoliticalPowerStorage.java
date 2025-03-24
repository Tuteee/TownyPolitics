package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.IPoliticalPowerStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLPoliticalPowerStorage implements IPoliticalPowerStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;

    public MySQLPoliticalPowerStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
    }

    @Override
    public void savePP(UUID nationUUID, double amount) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "political_power (nation_uuid, power_amount) " +
                             "VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE power_amount = ?")) {

            stmt.setString(1, nationUUID.toString());
            stmt.setDouble(2, amount);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save political power: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Double> loadAllPP() {
        Map<UUID, Double> result = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT nation_uuid, power_amount FROM " + prefix + "political_power")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("nation_uuid"));
                    double pp = rs.getDouble("power_amount");
                    result.put(uuid, pp);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load political power: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        // No specific action needed for MySQL as data is saved immediately
    }
}