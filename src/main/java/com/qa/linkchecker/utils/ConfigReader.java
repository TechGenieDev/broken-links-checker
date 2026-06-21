package com.qa.linkchecker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads src/test/resources/config.properties once and caches it.
 */
public class ConfigReader {

    private static Properties config;

    private ConfigReader() {
    }

    public static synchronized Properties getConfig() {
        if (config == null) {
            config = new Properties();
            try (InputStream is = ConfigReader.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {

                if (is == null) {
                    throw new RuntimeException(
                            "config.properties not found on classpath (expected at src/test/resources/config.properties)");
                }
                config.load(is);

            } catch (IOException e) {
                throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
            }
        }
        return config;
    }
}
