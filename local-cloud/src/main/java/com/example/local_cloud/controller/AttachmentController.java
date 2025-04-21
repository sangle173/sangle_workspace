package com.example.local_cloud.controller;

import com.example.local_cloud.model.Note;
import com.example.local_cloud.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentController.class);

    @Value("${notes.base-path}")
    private String basePath;

    @Autowired
    private NoteService noteService;

    @PostMapping("/upload/{space}/{category}/{noteId}")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable String space,
            @PathVariable String category,
            @PathVariable String noteId,
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        logger.info("Uploading file for note - space: {}, category: {}, noteId: {}", space, category, noteId);
        
        try {
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                logger.error("Note not found or folder name is null - noteId: {}", noteId);
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Note not found"));
            }

            // Create attachments directory in Desktop note folder
            Path attachmentsDir = Paths.get(basePath, space, category, note.getFolderName(), "attachments");
            Files.createDirectories(attachmentsDir);
            logger.info("Created attachments directory: {}", attachmentsDir);

            // Process filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "file_" + System.currentTimeMillis();
            }

            // Sanitize filename
            String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
            Path filePath = attachmentsDir.resolve(safeFilename);

            // Handle duplicate filenames
            int count = 1;
            String baseName = safeFilename;
            String extension = "";
            int dotIndex = safeFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = safeFilename.substring(0, dotIndex);
                extension = safeFilename.substring(dotIndex);
            }

            while (Files.exists(filePath)) {
                safeFilename = baseName + "_" + count + extension;
                filePath = attachmentsDir.resolve(safeFilename);
                count++;
            }

            // Save the file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved file to: {}", filePath);

            // Generate URL for accessing the file
            String url = String.format("/uploads/%s/%s/%s/attachments/%s", 
                space, category, note.getFolderName(), safeFilename);

            response.put("success", true);
            response.put("url", url);
            response.put("filename", safeFilename);
            response.put("type", file.getContentType());
            response.put("size", file.getSize());
            response.put("isImage", file.getContentType() != null && file.getContentType().startsWith("image/"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error uploading file", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/delete/{space}/{category}/{noteId}/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable String space,
            @PathVariable String category,
            @PathVariable String noteId,
            @PathVariable String filename) {
        
        try {
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Note not found or invalid"
                ));
            }

            Path filePath = Paths.get(basePath, space, category, note.getFolderName(), "attachments", filename);
            Files.deleteIfExists(filePath);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "File deleted successfully"
            ));

        } catch (Exception e) {
            logger.error("Error deleting file", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/list/{space}/{category}/{noteId}")
    public ResponseEntity<Map<String, Object>> listAttachments(
            @PathVariable String space,
            @PathVariable String category,
            @PathVariable String noteId) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Note not found"));
            }

            Path attachmentsDir = Paths.get(basePath, space, category, note.getFolderName(), "attachments");
            if (!Files.exists(attachmentsDir)) {
                return ResponseEntity.ok(Map.of("success", true, "attachments", new String[0]));
            }

            // List all files in attachments directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(attachmentsDir)) {
                Map<String, Object> attachments = new HashMap<>();
                for (Path file : stream) {
                    String fileName = file.getFileName().toString();
                    String url = String.format("/uploads/%s/%s/%s/attachments/%s", 
                        space, category, note.getFolderName(), fileName);
                    String contentType = Files.probeContentType(file);
                    
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", fileName);
                    fileInfo.put("url", url);
                    fileInfo.put("type", contentType);
                    fileInfo.put("size", Files.size(file));
                    fileInfo.put("isImage", contentType != null && contentType.startsWith("image/"));
                    
                    attachments.put(fileName, fileInfo);
                }
                
                response.put("success", true);
                response.put("attachments", attachments.values());
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("Error listing attachments", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
