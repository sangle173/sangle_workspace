package com.example.local_cloud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    private static final Path SETTINGS_PATH = Paths.get(System.getProperty("user.home"), "Desktop", "LocalCloudSettings.json");

    @GetMapping
    public String settingsPage(Model model) throws IOException {
        Map<String, Object> settings = getSettings();
        model.addAttribute("settings", settings);
        return "settings";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, Object> settings) throws IOException {
        Files.writeString(SETTINGS_PATH, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(settings));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api")
    @ResponseBody
    public Map<String, Object> getSettingsApi() throws IOException {
        return getSettings();
    }

    private Map<String, Object> getSettings() throws IOException {
        if (Files.exists(SETTINGS_PATH)) {
            String json = Files.readString(SETTINGS_PATH);
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, HashMap.class);
        }
        return new HashMap<>();
    }
}
