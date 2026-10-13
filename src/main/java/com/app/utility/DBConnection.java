package com.app.utility;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection manager using HikariCP connection pool.
 * 
 * Replaces the previous DriverManager-based implementation.
 * HikariCP provides:
 * - Connection pooling (avoids creating a new connection per request)
 * - Automatic connection validation and recovery
 * - Configurable pool size and timeouts
 * 
 * Configuration is loaded from app.properties on the classpath.
 * Copy app.properties.template to app.properties and fill in your credentials.
 */
public class DBConnection {

    private static volatile HikariDataSource dataSource;
    private static final Object LOCK = new Object();

    /**
     * Initializes the HikariCP pool. Called once at application startup
     * by AppLifecycleListener. Thread-safe (double-checked locking).
     */
    public static void init() {
        if (dataSource != null) return;
        synchronized (LOCK) {
            if (dataSource != null) return;
            try {
                Properties props = loadProperties();
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(props.getProperty("db.url",
                    "jdbc:mysql://localhost:3306/foodxpress?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
                config.setUsername(props.getProperty("db.username", "root"));
                config.setPassword(props.getProperty("db.password", ""));
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "10")));
                config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minimumIdle", "2")));
                config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
                config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
                config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
                config.setPoolName("FoodXpressPool");
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                dataSource = new HikariDataSource(config);
                System.out.println("[DBConnection] HikariCP pool initialized successfully.");
            } catch (Exception e) {
                System.err.println("[DBConnection] FATAL: Failed to initialize connection pool!");
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize database connection pool", e);
            }
        }
    }

    /**
     * Returns a pooled JDBC connection. The caller MUST close it
     * (use try-with-resources) to return it to the pool.
     *
     * @return a database connection from the pool
     * @throws SQLException if a connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            // Lazy init fallback for test contexts that don't go through the servlet listener
            init();
        }
        return dataSource.getConnection();
    }

    /**
     * Shuts down the connection pool cleanly. Called at application shutdown
     * by AppLifecycleListener.contextDestroyed().
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DBConnection] HikariCP pool closed.");
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            if (in == null) {
                System.err.println("[DBConnection] WARNING: app.properties not found on classpath. " +
                    "Copy app.properties.template to src/main/resources/app.properties and fill in credentials.");
                return props; // Return empty; HikariConfig will use defaults
            }
            props.load(in);
        }
        return props;
    }

    private DBConnection() { /* static utility class */ }
}
