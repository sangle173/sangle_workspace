package com.example.local_cloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class EnvConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);
    private static final String ENV_FILE_PATH = System.getProperty("user.home") + "/Desktop/Env/.env";
    
    private final Map<String, String> envVars = new HashMap<>();
    
    @PostConstruct
    public void init() {
        loadEnvFile();
    }
    
    private void loadEnvFile() {
        File envFile = new File(ENV_FILE_PATH);
        
        if (!envFile.exists()) {
            logger.warn("Env file not found at: {}. Creating empty file.", ENV_FILE_PATH);
            try {
                // Create directory if it doesn't exist
                File parentDir = envFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // Create empty env file
                if (envFile.createNewFile()) {
                    logger.info("Created empty .env file at: {}", ENV_FILE_PATH);
                }
            } catch (IOException e) {
                logger.error("Failed to create .env file: {}", e.getMessage());
            }
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse key=value pairs
                int equalsIdx = line.indexOf('=');
                if (equalsIdx > 0) {
                    String key = line.substring(0, equalsIdx).trim();
                    String value = line.substring(equalsIdx + 1).trim();
                    
                    // Remove quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) || 
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    envVars.put(key, value);
                    logger.debug("Loaded env variable: {}", key);
                }
            }
            logger.info("Successfully loaded {} environment variables from: {}", envVars.size(), ENV_FILE_PATH);
        } catch (IOException e) {
            logger.error("Error reading .env file: {}", e.getMessage());
        }
    }
    
    /**
     * Get an environment variable value from the .env file
     * @param key The variable name
     * @param defaultValue Default value if variable is not defined
     * @return The value or the default value if not found
     */
    public String getEnvValue(String key, String defaultValue) {
        return envVars.getOrDefault(key, defaultValue);
    }
    
    /**
     * Check if an environment variable exists in the .env file
     * @param key The variable name
     * @return true if the variable exists, false otherwise
     */
    public boolean hasEnvValue(String key) {
        return envVars.containsKey(key);
    }
} 