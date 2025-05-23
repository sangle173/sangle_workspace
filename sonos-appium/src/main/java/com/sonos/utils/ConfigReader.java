package com.sonos.utils;

import java.io.FileInputStream;
import java.util.Properties;

public class ConfigReader {
    private static Properties prop;

    public static Properties getProperties() {
        if (prop == null) {
            try {
                prop = new Properties();
                prop.load(new FileInputStream("src/test/resources/config.properties"));
            } catch (Exception e) {
                throw new RuntimeException("Unable to load config.properties");
            }
        }
        return prop;
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        return sys != null ? sys : getProperties().getProperty(key);
    }
}
