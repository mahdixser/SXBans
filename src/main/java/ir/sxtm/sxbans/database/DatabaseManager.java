package ir.sxtm.sxbans.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.config.ConfigManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DatabaseManager {
    private final SXBans plugin;
    private final ConfigManager config;
    private HikariDataSource dataSource;
    private DatabaseType type;
    private final Map<String, PreparedStatement> preparedStatements;
    private boolean isConnected;

    public DatabaseManager(SXBans plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.preparedStatements = new ConcurrentHashMap<>();
        this.isConnected = false;
    }

    public void initialize() {
        String dbType = config.getDatabaseType().toLowerCase();
        this.type = DatabaseType.fromString(dbType);

        if (type == DatabaseType.JSON) {
            plugin.getSXBansLogger().info("Using JSON storage (no database connection needed)");
            isConnected = true;
            return;
        }

        if (!DriverLoader.ensureDriver(plugin, type)) {
            isConnected = false;
            plugin.getSXBansLogger().severe("Database disabled: JDBC driver for " + type.name() + " is not available.");
            return;
        }

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(getDriverClass());
            hikariConfig.setJdbcUrl(getJdbcUrl());

            if (type != DatabaseType.SQLITE && type != DatabaseType.H2) {
                hikariConfig.setUsername(config.getString("database.username", ""));
                hikariConfig.setPassword(config.getString("database.password", ""));
            }

            hikariConfig.setMaximumPoolSize(20);
            hikariConfig.setMinimumIdle(5);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setLeakDetectionThreshold(60000);

            if (type == DatabaseType.MYSQL) {
                hikariConfig.addDataSourceProperty("useSSL", "false");
                hikariConfig.addDataSourceProperty("serverTimezone", "UTC");
                hikariConfig.addDataSourceProperty("useUnicode", "true");
                hikariConfig.addDataSourceProperty("characterEncoding", "UTF-8");
            } else if (type == DatabaseType.POSTGRESQL) {
                hikariConfig.addDataSourceProperty("ssl", "false");
            }

            this.dataSource = new HikariDataSource(hikariConfig);

            try (Connection conn = dataSource.getConnection()) {
                isConnected = true;
                plugin.getSXBansLogger().info("Database connected successfully: " + type.name());
            }

        } catch (Exception e) {
            isConnected = false;
            plugin.getSXBansLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getDriverClass() {
        switch (type) {
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case SQLITE:
                return "org.sqlite.JDBC";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case H2:
                return "org.h2.Driver";
            default:
                return "";
        }
    }

    private String getJdbcUrl() {
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String name = config.getString("database.name", "sxbans");

        switch (type) {
            case MYSQL:
                return "jdbc:mysql://" + host + ":" + port + "/" + name + "?autoReconnect=true&useSSL=false";
            case SQLITE:
                return "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/data.db";
            case POSTGRESQL:
                return "jdbc:postgresql://" + host + ":" + port + "/" + name;
            case H2:
                return "jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/data;DB_CLOSE_ON_EXIT=FALSE";
            default:
                return "";
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected) {
            throw new SQLException("Database is not connected");
        }
        if (type == DatabaseType.JSON) {
            throw new SQLException("JSON storage does not support direct connections");
        }
        return dataSource.getConnection();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (!isConnected || type == DatabaseType.JSON) {
            throw new SQLException("Database is not connected or using JSON storage");
        }

        PreparedStatement stmt = preparedStatements.get(sql);
        if (stmt == null || stmt.isClosed()) {
            stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatements.put(sql, stmt);
        }
        return stmt;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            isConnected = false;
            plugin.getSXBansLogger().info("Database connection closed");
        }

        for (PreparedStatement stmt : preparedStatements.values()) {
            try {
                if (!stmt.isClosed()) {
                    stmt.close();
                }
            } catch (SQLException e) {

            }
        }
        preparedStatements.clear();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public DatabaseType getType() {
        return type;
    }

    public enum DatabaseType {
        JSON("json"),
        MYSQL("mysql"),
        SQLITE("sqlite"),
        POSTGRESQL("postgresql"),
        H2("h2");

        private final String name;

        DatabaseType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static DatabaseType fromString(String type) {
            for (DatabaseType dbType : values()) {
                if (dbType.getName().equalsIgnoreCase(type)) {
                    return dbType;
                }
            }
            return JSON;
        }
    }
}