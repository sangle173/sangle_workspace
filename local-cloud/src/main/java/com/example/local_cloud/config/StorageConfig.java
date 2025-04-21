package com.example.local_cloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {
    private static final Logger logger = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${notes.base-path}")
    private String basePath;

    @PostConstruct
    public void init() {
        try {
            // Ensure base directory exists
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                logger.info("Creating base directory at: {}", baseDir);
                Files.createDirectories(baseDir);
            }

            // Verify directory permissions
            if (!Files.isWritable(baseDir)) {
                throw new RuntimeException("Base directory is not writable: " + baseDir);
            }

            // Create standard subdirectories if they don't exist
            createSubDirectoryIfNotExists(baseDir, "SangLe");
            createSubDirectoryIfNotExists(baseDir, "Sonos Note");

            logger.info("Storage initialized successfully at: {}", baseDir);
            logger.info("Desktop note directory structure ready for use");
        } catch (IOException e) {
            String errorMsg = "Could not initialize storage: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    private void createSubDirectoryIfNotExists(Path parent, String dirName) throws IOException {
        Path dir = parent.resolve(dirName);
        if (!Files.exists(dir)) {
            logger.info("Creating subdirectory: {}", dir);
            Files.createDirectories(dir);
        }
    }
}