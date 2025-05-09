package com.example.local_cloud.controller;

import com.example.local_cloud.config.EnvConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/env")
public class EnvController {

    private final EnvConfig envConfig;
    
    @Autowired
    public EnvController(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEnvStatus() {
        Map<String, Object> response = new HashMap<>();
        
        // Check Jira token
        boolean hasJiraToken = envConfig.hasEnvValue("JIRA_API_TOKEN");
        
        if (hasJiraToken) {
            response.put("status", "ok");
            response.put("message", "Environment variables are properly configured");
        } else {
            response.put("status", "error");
            response.put("message", "JIRA API token is missing in the .env file");
            response.put("missingVars", new String[]{"JIRA_API_TOKEN"});
        }
        
        return ResponseEntity.ok(response);
    }
} 