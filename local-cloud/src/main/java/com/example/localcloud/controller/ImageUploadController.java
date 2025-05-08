package com.example.localcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ImageUploadController {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("upload") MultipartFile file) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Return URL
            String url = "/uploads/" + filename;
            Map<String, Object> response = new HashMap<>();
            response.put("url", url);
            response.put("uploaded", true);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("uploaded", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 