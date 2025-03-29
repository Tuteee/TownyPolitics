package com.orbismc.townyPolitics;

import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {

    private final TownyPolitics plugin;
    private final boolean useMySQL;
    private HikariDataSource dataSource;
    private String prefix;
    private final DelegateLogger logger;

    public DatabaseManager(TownyPolitics plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfig().getBoolean("database.use_mysql", false);
        this.prefix = plugin.getConfig().getString("database.prefix", "tp_");
        this.logger = new DelegateLogger(plugin, "Database");

        if (useMySQL) {
            setupMySQL();
        } else {
            logger.info("MySQL is disabled, using YAML storage");
        }
    }

    private void setupMySQL() {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "townypolitics");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "password");
        int poolSize = plugin.getConfig().getInt("database.connection_pool_size", 10);
        long maxLifetime = plugin.getConfig().getLong("database.max_lifetime", 1800000);

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";

        logger.info("Connecting to MySQL at " + host + ":" + port + "/" + database);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTestQuery("SELECT 1");

        try {
            dataSource = new HikariDataSource(config);
            logger.info("Successfully connected to MySQL database!");
            createTables();
        } catch (Exception e) {
            logger.severe("Failed to connect to MySQL database: " + e.getMessage());
            logger.severe("Falling back to YAML storage...");
        }
    }

    private void createTables() {
        // Create tables if they don't exist
        try (Connection conn = getConnection()) {
            // Political Power table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "political_power (" +
                            "nation_uuid VARCHAR(36) PRIMARY KEY, " +
                            "power_amount DOUBLE NOT NULL)")) {
                stmt.executeUpdate();
                logger.fine("Created/verified political_power table");
            }

            // Town Political Power table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "town_political_power (" +
                            "town_uuid VARCHAR(36) PRIMARY KEY, " +
                            "power_amount DOUBLE NOT NULL)")) {
                stmt.executeUpdate();
                logger.fine("Created/verified town_political_power table");
            }

            // Governments table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "governments (" +
                            "entity_uuid VARCHAR(36) PRIMARY KEY, " +
                            "entity_type ENUM('TOWN', 'NATION') NOT NULL, " +
                            "government_type VARCHAR(50) NOT NULL, " +
                            "last_change_time BIGINT NOT NULL)")) {
                stmt.executeUpdate();
                logger.fine("Created/verified governments table");
            }

            // Corruption table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "corruption (" +
                            "nation_uuid VARCHAR(36) PRIMARY KEY, " +
                            "corruption_amount DOUBLE NOT NULL)")) {
                stmt.executeUpdate();
                logger.fine("Created/verified corruption table");
            }

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
                logger.fine("Created/verified active_policies table");
            }

            logger.info("Successfully created/verified all database tables!");
        } catch (SQLException e) {
            logger.severe("Failed to create database tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (!useMySQL || dataSource == null) {
            throw new SQLException("MySQL is not enabled or connection failed");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
        }
    }

    public boolean isUsingMySQL() {
        return useMySQL && dataSource != null;
    }

    public String getPrefix() {
        return prefix;
    }
}