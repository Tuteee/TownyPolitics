package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.budget.BudgetAllocation;
import com.orbismc.townyPolitics.budget.BudgetCategory;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.IBudgetStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.EnumMap;

public class MySQLBudgetStorage extends AbstractMySQLStorage implements IBudgetStorage {

    public MySQLBudgetStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLBudgetStorage");
        createTables();
        logger.info("MySQL Budget Storage initialized");
    }

    /**
     * Create the budget tables if they don't exist
     */
    private void createTables() {
        try (Connection conn = getConnection()) {
            // Budget allocations table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "budget_allocations (" +
                            "entity_uuid VARCHAR(36) NOT NULL, " +
                            "is_nation BOOLEAN NOT NULL, " +
                            "category VARCHAR(20) NOT NULL, " +
                            "percentage DOUBLE NOT NULL, " +
                            "priority INT NOT NULL, " +
                            "PRIMARY KEY (entity_uuid, is_nation, category))")) {
                stmt.executeUpdate();
                logger.fine("Created/verified budget_allocations table");
            }

            // Budget cycle times table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "budget_cycles (" +
                            "entity_uuid VARCHAR(36) NOT NULL, " +
                            "is_nation BOOLEAN NOT NULL, " +
                            "last_cycle_time BIGINT NOT NULL, " +
                            "PRIMARY KEY (entity_uuid, is_nation))")) {
                stmt.executeUpdate();
                logger.fine("Created/verified budget_cycles table");
            }

            logger.info("Successfully created/verified budget database tables");
        } catch (SQLException e) {
            logger.severe("Failed to create budget database tables: " + e.getMessage());
        }
    }

    @Override
    public void saveBudgetAllocation(UUID entityId, BudgetCategory category, BudgetAllocation allocation, boolean isNation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "budget_allocations " +
                             "(entity_uuid, is_nation, category, percentage, priority) " +
                             "VALUES (?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE percentage = ?, priority = ?")) {

            stmt.setString(1, entityId.toString());
            stmt.setBoolean(2, isNation);
            stmt.setString(3, category.name());
            stmt.setDouble(4, allocation.getPercentage());
            stmt.setInt(5, allocation.getPriority());
            stmt.setDouble(6, allocation.getPercentage());
            stmt.setInt(7, allocation.getPriority());

            stmt.executeUpdate();
            logger.fine("Saved budget allocation for " + (isNation ? "nation" : "town") +
                    " " + entityId + ": " + category.name() + " = " + allocation.getPercentage() + "%");
        } catch (SQLException e) {
            logger.severe("Failed to save budget allocation: " + e.getMessage());
        }
    }

    @Override
    public void saveLastBudgetCycle(UUID entityId, long timestamp, boolean isNation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "budget_cycles " +
                             "(entity_uuid, is_nation, last_cycle_time) " +
                             "VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE last_cycle_time = ?")) {

            stmt.setString(1, entityId.toString());
            stmt.setBoolean(2, isNation);
            stmt.setLong(3, timestamp);
            stmt.setLong(4, timestamp);

            stmt.executeUpdate();
            logger.fine("Saved last budget cycle for " + (isNation ? "nation" : "town") +
                    " " + entityId + ": " + timestamp);
        } catch (SQLException e) {
            logger.severe("Failed to save last budget cycle: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Map<BudgetCategory, BudgetAllocation>> loadAllBudgetAllocations(boolean isNation) {
        Map<UUID, Map<BudgetCategory, BudgetAllocation>> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, category, percentage, priority " +
                             "FROM " + prefix + "budget_allocations " +
                             "WHERE is_nation = ?")) {

            stmt.setBoolean(1, isNation);
            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID entityId = UUID.fromString(rs.getString("entity_uuid"));
                    String categoryName = rs.getString("category");
                    double percentage = rs.getDouble("percentage");
                    int priority = rs.getInt("priority");

                    BudgetCategory category = BudgetCategory.valueOf(categoryName);
                    BudgetAllocation allocation = new BudgetAllocation(percentage, priority);

                    Map<BudgetCategory, BudgetAllocation> entityBudget = result.computeIfAbsent(
                            entityId, k -> new EnumMap<>(BudgetCategory.class));
                    entityBudget.put(category, allocation);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in budget allocations: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " budget allocations for " +
                    (isNation ? "nations" : "towns"));
        } catch (SQLException e) {
            logger.severe("Failed to load budget allocations: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<UUID, Long> loadAllLastBudgetCycles(boolean isNation) {
        Map<UUID, Long> result = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT entity_uuid, last_cycle_time " +
                             "FROM " + prefix + "budget_cycles " +
                             "WHERE is_nation = ?")) {

            stmt.setBoolean(1, isNation);
            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID entityId = UUID.fromString(rs.getString("entity_uuid"));
                    long timestamp = rs.getLong("last_cycle_time");

                    result.put(entityId, timestamp);
                    count++;
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in budget cycles: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " budget cycle times for " +
                    (isNation ? "nations" : "towns"));
        } catch (SQLException e) {
            logger.severe("Failed to load budget cycle times: " + e.getMessage());
        }

        return result;
    }
}