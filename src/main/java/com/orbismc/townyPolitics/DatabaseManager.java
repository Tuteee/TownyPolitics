package com.orbismc.townyPolitics;

import com.orbismc.townyPolitics.TownyPolitics;
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

    public DatabaseManager(TownyPolitics plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfig().getBoolean("database.use_mysql", false);
        this.prefix = plugin.getConfig().getString("database.prefix", "tp_");

        if (useMySQL) {
            setupMySQL();
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

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTestQuery("SELECT 1");

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Successfully connected to MySQL database!");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            plugin.getLogger().severe("Falling back to YAML storage...");
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
            }

            // Governments table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "governments (" +
                            "entity_uuid VARCHAR(36) PRIMARY KEY, " +
                            "entity_type ENUM('TOWN', 'NATION') NOT NULL, " +
                            "government_type VARCHAR(50) NOT NULL, " +
                            "last_change_time BIGINT NOT NULL)")) {
                stmt.executeUpdate();
            }

            // Corruption table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " + prefix + "corruption (" +
                            "nation_uuid VARCHAR(36) PRIMARY KEY, " +
                            "corruption_amount DOUBLE NOT NULL)")) {
                stmt.executeUpdate();
            }

            plugin.getLogger().info("Successfully created database tables!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
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