package com.netroute.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    @Test
    void defaultConfigurationHasSensibleValues() {
        DatabaseConfig config = new DatabaseConfig(
                DatabaseConfig.DEFAULT_URL,
                DatabaseConfig.DEFAULT_USER,
                DatabaseConfig.DEFAULT_PASSWORD,
                DatabaseConfig.DEFAULT_TIMEOUT_SECONDS,
                "Test"
        );

        assertTrue(config.getUrl().contains("netroute_db"));
        assertEquals("root", config.getUser());
        assertEquals("", config.getPassword());
        assertEquals(5, config.getTimeoutSeconds());
        assertEquals("Test", config.getSourceDescription());
    }

    @Test
    void maskPasswordNeverExposesPlaintext() {
        DatabaseConfig config = new DatabaseConfig(
                DatabaseConfig.DEFAULT_URL,
                "admin",
                "SuperSecretPassword123!",
                5,
                "SecureTest"
        );

        String str = config.toString();
        assertFalse(str.contains("SuperSecretPassword123!"));
        assertTrue(str.contains("********"));
        assertTrue(str.contains("admin"));
    }

    @Test
    void loadsCustomPropertiesFileCorrectly(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("custom-db.properties");
        String content = "db.url=jdbc:mysql://127.0.0.1:3307/custom_netroute\n"
                + "db.user=net_user\n"
                + "db.password=net_pass\n"
                + "db.timeout=10\n";
        Files.writeString(propsFile, content);

        DatabaseConfig config = DatabaseConfig.loadFromFile(propsFile);
        assertNotNull(config);
        assertEquals("jdbc:mysql://127.0.0.1:3307/custom_netroute", config.getUrl());
        assertEquals("net_user", config.getUser());
        assertEquals("net_pass", config.getPassword());
        assertEquals(10, config.getTimeoutSeconds());
        assertTrue(config.getSourceDescription().contains("custom-db.properties"));
    }

    @Test
    void returnsNullOnMissingPropertiesFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("non-existent.properties");
        DatabaseConfig config = DatabaseConfig.loadFromFile(missing);
        assertEquals(null, config);
    }
}
