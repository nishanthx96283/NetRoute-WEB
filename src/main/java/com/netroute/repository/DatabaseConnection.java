package com.netroute.repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Factory and diagnostic manager for JDBC connections to MySQL.
 */
public final class DatabaseConnection {

    private final DatabaseConfig config;

    public DatabaseConnection() {
        this(DatabaseConfig.load());
    }

    public DatabaseConnection(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "config is required");
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    /**
     * Opens and returns a new JDBC {@link Connection}.
     * Callers must close the connection, typically within a try-with-resources statement.
     */
    public Connection getConnection() throws SQLException {
        DriverManager.setLoginTimeout(config.getTimeoutSeconds());
        return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    }

    /**
     * Performs an active connection check and returns diagnostic details without throwing.
     */
    public ConnectionResult testConnection() {
        long start = System.currentTimeMillis();
        try {
            DriverManager.setLoginTimeout(config.getTimeoutSeconds());
            try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword())) {
                boolean valid = conn.isValid(config.getTimeoutSeconds());
                long elapsed = System.currentTimeMillis() - start;
                if (!valid) {
                    return new ConnectionResult(false, "Connection was established but failed validation check.", null, null, elapsed, null);
                }
                DatabaseMetaData meta = conn.getMetaData();
                String product = meta.getDatabaseProductName();
                String version = meta.getDatabaseProductVersion();
                return new ConnectionResult(true, "Successfully connected to MySQL database: " + product + " " + version,
                        product, version, elapsed, null);
            }
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            String msg = "Connection failed: " + e.getMessage();
            return new ConnectionResult(false, msg, null, null, elapsed, e);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new ConnectionResult(false, "Unexpected error: " + e.getMessage(), null, null, elapsed, e);
        }
    }

    /**
     * Convenience method checking whether the database is reachable within timeout.
     */
    public boolean isAvailable() {
        return testConnection().isSuccess();
    }

    /**
     * Diagnostic result of a database connection test.
     */
    public static final class ConnectionResult {
        private final boolean success;
        private final String message;
        private final String databaseProductName;
        private final String databaseProductVersion;
        private final long responseTimeMs;
        private final Exception exception;

        public ConnectionResult(
                boolean success,
                String message,
                String databaseProductName,
                String databaseProductVersion,
                long responseTimeMs,
                Exception exception) {
            this.success = success;
            this.message = message;
            this.databaseProductName = databaseProductName;
            this.databaseProductVersion = databaseProductVersion;
            this.responseTimeMs = responseTimeMs;
            this.exception = exception;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getDatabaseProductName() {
            return databaseProductName;
        }

        public String getDatabaseProductVersion() {
            return databaseProductVersion;
        }

        public long getResponseTimeMs() {
            return responseTimeMs;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public String toString() {
            if (success) {
                return "Connection Successful (" + databaseProductName + " " + databaseProductVersion
                        + ", " + responseTimeMs + "ms)";
            }
            return "Connection Failed: " + message;
        }
    }

    /**
     * Standalone CLI diagnostic runner to verify MySQL configuration from the terminal.
     */
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("NetRoute Database Connection Diagnostic Tool");
        System.out.println("==================================================");
        DatabaseConnection conn = new DatabaseConnection();
        System.out.println("Configuration Source : " + conn.getConfig().getSourceDescription());
        System.out.println("Target Database URL  : " + conn.getConfig().getUrl());
        System.out.println("Target Username      : " + conn.getConfig().getUser());
        System.out.println("Config File Present  : " + (DatabaseConfig.isLocalConfigFilePresent() ? "Yes (db.properties)" : "No (using defaults/env)"));
        System.out.println("\nTesting connection to MySQL server...");
        ConnectionResult result = conn.testConnection();
        System.out.println("Status: " + (result.isSuccess() ? "[SUCCESS]" : "[FAILED]"));
        System.out.println("Details: " + result.getMessage());
        if (!result.isSuccess()) {
            System.out.println("\nSetup Steps:");
            System.out.println("1. Start MySQL Server (e.g. net start MySQL or via MySQL Service).");
            System.out.println("2. Execute database-schema.sql in MySQL Command Line Client:");
            System.out.println("   mysql> source D:/NetRoute/database-schema.sql");
            System.out.println("3. Copy db.properties.example to db.properties and set your local password:");
            System.out.println("   copy db.properties.example db.properties");
        }
        System.out.println("==================================================");
    }
}
