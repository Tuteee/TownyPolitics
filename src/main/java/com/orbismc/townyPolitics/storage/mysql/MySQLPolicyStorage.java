package com.orbismc.townyPolitics.storage.mysql;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.storage.IPolicyStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MySQLPolicyStorage implements IPolicyStorage {

    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final String prefix;
    private final DelegateLogger logger;

    public MySQLPolicyStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.prefix = dbManager.getPrefix();
        this.logger = new DelegateLogger(plugin, "MySQLPolicyStorage");

        // Create tables if they don't exist
        createTables();
        logger.info("MySQL Policy Storage initialized");
    }

    /**
     * Create necessary tables if they don't exist
     */
    private void createTables() {
        try (Connection conn = dbManager.getConnection()) {
            // Active Policies table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "active_policies (" +
                            "id VARCHAR(36) PRIMARY KEY, " +
                            "policy_id VARCHAR(50) NOT NULL, " +
                            "entity_uuid VARCHAR(36) NOT NULL, " +
                            "is_nation BOOLEAN NOT NULL, " +
                            "enacted_time BIGINT NOT NULL, " +
                            "expiry_time BIGINT NOT NULL, " +
                            "INDEX (entity_uuid, is_nation))")) {
                stmt.executeUpdate();
                logger.info("Active Policies table created or verified");
            }

        } catch (SQLException e) {
            logger.severe("Failed to create policy tables: " + e.getMessage());
        }
    }

    @Override
    public void saveActivePolicy(ActivePolicy policy) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + prefix + "active_policies " +
                             "(id, policy_id, entity_uuid, is_nation, enacted_time, expiry_time) " +
                             "VALUES (?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "policy_id = ?, entity_uuid = ?, is_nation = ?, " +
                             "enacted_time = ?, expiry_time = ?")) {

            stmt.setString(1, policy.getId().toString());
            stmt.setString(2, policy.getPolicyId());
            stmt.setString(3, policy.getEntityId().toString());
            stmt.setBoolean(4, policy.isNation());
            stmt.setLong(5, policy.getEnactedTime());
            stmt.setLong(6, policy.getExpiryTime());

            // For ON DUPLICATE KEY UPDATE
            stmt.setString(7, policy.getPolicyId());
            stmt.setString(8, policy.getEntityId().toString());
            stmt.setBoolean(9, policy.isNation());
            stmt.setLong(10, policy.getEnactedTime());
            stmt.setLong(11, policy.getExpiryTime());

            int updated = stmt.executeUpdate();
            logger.fine("Saved active policy: " + policy.getId() + " (rows affected: " + updated + ")");

        } catch (SQLException e) {
            logger.severe("Failed to save active policy: " + e.getMessage());
        }
    }

    @Override
    public void removeActivePolicy(UUID policyId) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + prefix + "active_policies WHERE id = ?")) {

            stmt.setString(1, policyId.toString());

            int updated = stmt.executeUpdate();
            logger.fine("Removed active policy: " + policyId + " (rows affected: " + updated + ")");

        } catch (SQLException e) {
            logger.severe("Failed to remove active policy: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Set<ActivePolicy>> loadActivePolicies(boolean isNation) {
        Map<UUID, Set<ActivePolicy>> result = new ConcurrentHashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM " + prefix + "active_policies WHERE is_nation = ?")) {

            stmt.setBoolean(1, isNation);

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String policyId = rs.getString("policy_id");
                    UUID entityId = UUID.fromString(rs.getString("entity_uuid"));
                    long enactedTime = rs.getLong("enacted_time");
                    long expiryTime = rs.getLong("expiry_time");

                    ActivePolicy policy = new ActivePolicy(
                            id, policyId, entityId, isNation, enactedTime, expiryTime);

                    // Skip expired policies
                    if (policy.isExpired()) {
                        // Remove from database
                        removeActivePolicy(id);
                        continue;
                    }

                    // Add to result map
                    Set<ActivePolicy> policies = result.computeIfAbsent(entityId, k -> new HashSet<>());
                    policies.add(policy);
                    count++;

                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in database: " + e.getMessage());
                }
            }

            logger.info("Loaded " + count + " active policies for " +
                    (isNation ? "nations" : "towns") + " from database");

        } catch (SQLException e) {
            logger.severe("Failed to load active policies: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveAll() {
        // No specific action needed for MySQL as data is saved immediately
        logger.fine("saveAll() called (no action needed for MySQL storage)");
    }
}