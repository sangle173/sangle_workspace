package com.example.local_cloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${notes.base-path}")
    private String basePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure the path ends with a separator
        String path = basePath;
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }

        // Log the upload path for debugging
        logger.info("Serving uploaded files from: {}", path);

        // Serve files from the notes directory (/uploads/**)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);

        // Serve cut video files from a separate folder (/cuts/**)
        String cutPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "LocalCloudCuts" + File.separator;
        logger.info("Serving cut videos from: {}", cutPath);

        registry.addResourceHandler("/cuts/**")
                .addResourceLocations("file:" + cutPath);
    }
}
