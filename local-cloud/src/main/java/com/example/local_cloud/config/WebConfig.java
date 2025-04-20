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
        
        // Log the path for debugging
        logger.info("Serving files from: {}", path);
        
        // Serve files from the notes directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }
} 