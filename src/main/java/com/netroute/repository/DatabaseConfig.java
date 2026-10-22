package com.netroute.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration holder for MySQL database connections.
 * <p>
 * Resolution priority:
 * <ol>
 *   <li>System Environment Variables ({@code NETROUTE_DB_URL}, {@code NETROUTE_DB_USER}, {@code NETROUTE_DB_PASSWORD})</li>
 *   <li>Local file {@code db.properties} in working directory or user home</li>
 *   <li>Standard defaults (localhost:3306/netroute_db, user root)</li>
 * </ol>
 * Real credentials are never stored in source code or committed to Git.
 */
public final class DatabaseConfig {

    public static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/netroute_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "";
    public static final int DEFAULT_TIMEOUT_SECONDS = 5;

    public static final String ENV_URL = "NETROUTE_DB_URL";
    public static final String ENV_USER = "NETROUTE_DB_USER";
    public static final String ENV_PASSWORD = "NETROUTE_DB_PASSWORD";

    public static final String CONFIG_FILE_NAME = "db.properties";

    private final String url;
    private final String user;
    private final String password;
    private final int timeoutSeconds;
    private final String sourceDescription;

    public DatabaseConfig(String url, String user, String password, int timeoutSeconds, String sourceDescription) {
        this.url = Objects.requireNonNull(url, "url is required").trim();
        this.user = Objects.requireNonNull(user, "user is required").trim();
        this.password = password == null ? "" : password;
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.sourceDescription = sourceDescription == null ? "Explicit" : sourceDescription;
    }

    /**
     * Loads database configuration using standard precedence: Environment Variables -> db.properties -> Defaults.
     */
    public static DatabaseConfig load() {
        // 1. Check environment variables
        String envUrl = System.getenv(ENV_URL);
        String envUser = System.getenv(ENV_USER);
        String envPass = System.getenv(ENV_PASSWORD);

        if (envUrl != null && !envUrl.isBlank()) {
            String user = (envUser != null && !envUser.isBlank()) ? envUser : DEFAULT_USER;
            String pass = envPass != null ? envPass : DEFAULT_PASSWORD;
            return new DatabaseConfig(envUrl, user, pass, DEFAULT_TIMEOUT_SECONDS, "Environment Variables");
        }

        // 2. Check local db.properties in current directory
        Path localPath = Paths.get(CONFIG_FILE_NAME);
        if (Files.exists(localPath)) {
            DatabaseConfig fromFile = loadFromFile(localPath);
            if (fromFile != null) {
                return fromFile;
            }
        }

        // 3. Fallback to default localhost configuration
        return new DatabaseConfig(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD, DEFAULT_TIMEOUT_SECONDS, "Default Localhost");
    }

    /**
     * Loads configuration from a specific properties file path.
     */
    public static DatabaseConfig loadFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            String url = props.getProperty("db.url", DEFAULT_URL);
            String user = props.getProperty("db.user", DEFAULT_USER);
            String pass = props.getProperty("db.password", DEFAULT_PASSWORD);
            int timeout = DEFAULT_TIMEOUT_SECONDS;
            try {
                String timeoutStr = props.getProperty("db.timeout");
                if (timeoutStr != null && !timeoutStr.isBlank()) {
                    timeout = Integer.parseInt(timeoutStr.trim());
                }
            } catch (NumberFormatException ignored) {
                // Keep default timeout
            }
            return new DatabaseConfig(url, user, pass, timeout, "File: " + path.toAbsolutePath());
        } catch (IOException e) {
            return null;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    /**
     * Returns true if a local db.properties file exists.
     */
    public static boolean isLocalConfigFilePresent() {
        return Files.exists(Paths.get(CONFIG_FILE_NAME));
    }

    /**
     * Masked string representation safe for logging (never prints the password).
     */
    @Override
    public String toString() {
        String maskedPass = (password == null || password.isEmpty()) ? "<empty>" : "********";
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=" + maskedPass
                + ", timeout=" + timeoutSeconds + "s, source=" + sourceDescription + "]";
    }
}
