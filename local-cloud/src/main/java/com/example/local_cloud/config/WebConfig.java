package com.example.local_cloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve cut video files from a separate folder (/cuts/**)
        String cutPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "LocalCloudCuts" + File.separator;
        logger.info("Serving cut videos from: {}", cutPath);

        registry.addResourceHandler("/cuts/**")
                .addResourceLocations("file:" + cutPath);
    }
}
