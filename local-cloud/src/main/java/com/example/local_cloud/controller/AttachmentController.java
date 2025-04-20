package com.example.local_cloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/uploads")
public class AttachmentController {

    @Value("${notes.base-path}")
    private String basePath;

    @PostMapping("/{space}/{category}/{noteId}/upload")
    public ResponseEntity<Map<String, String>> uploadAttachment(@PathVariable String space,
                                                                @PathVariable String category,
                                                                @PathVariable String noteId,
                                                                @RequestParam("file") MultipartFile file) {
        try {
            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String filename = timestamp + "_" + original;

            Path uploadDir = Paths.get(basePath, space, category, noteId, "upload");
            Files.createDirectories(uploadDir);

            Path savePath = uploadDir.resolve(filename);
            file.transferTo(savePath);

            String url = "/uploads/" + space + "/" + category + "/" + noteId + "/upload/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
