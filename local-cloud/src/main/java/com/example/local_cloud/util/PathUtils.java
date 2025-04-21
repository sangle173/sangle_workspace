package com.example.local_cloud.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {
    private static final Logger logger = LoggerFactory.getLogger(PathUtils.class);

    public static String getDesktopPath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        Path desktopPath;
        if (os.contains("win")) {
            desktopPath = Paths.get(userHome, "Desktop");
        } else if (os.contains("mac")) {
            desktopPath = Paths.get(userHome, "Desktop");
        } else {
            // Linux and others
            desktopPath = Paths.get(userHome, "Desktop");
            // Some Linux distros might use a localized name
            if (!desktopPath.toFile().exists()) {
                desktopPath = Paths.get(userHome, "Desktop");
            }
        }
        
        String path = desktopPath.resolve("note").toString();
        logger.info("Resolved desktop note path: {}", path);
        return path;
    }
}