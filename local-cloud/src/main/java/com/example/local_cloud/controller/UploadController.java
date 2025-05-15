package com.example.local_cloud.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.attribute.FileTime;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;

@Controller
public class UploadController {

    private static final String DESKTOP = System.getProperty("user.home") + File.separator + "Desktop";
    private static final String UPLOAD_DIR = DESKTOP + File.separator + "LocalCloudUploads";
    private static final String TEMP_DIR = DESKTOP + File.separator + "LocalCloudTemp";

    static {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            Files.createDirectories(Paths.get(TEMP_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @GetMapping("/")
    public String showHome(Model model) {
        List<FileInfo> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(UPLOAD_DIR))) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String name = entry.getFileName().toString();
                    String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
                    FileTime lastModifiedTime = Files.getLastModifiedTime(entry);
                    files.add(new FileInfo(
                            name,
                            ext,
                            Files.size(entry),
                            lastModifiedTime.toMillis()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        files.sort(Comparator.comparingLong(FileInfo::getTimestamp).reversed());
        model.addAttribute("files", files);
        return "index";
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.status(400).body("File name cannot be null");
            }
            Path dest = Paths.get(UPLOAD_DIR, originalFilename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok("Uploaded: " + originalFilename);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = Paths.get(UPLOAD_DIR, filename);
            if (!Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file.toFile());
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/open-uploadsfolder")
    @ResponseBody
    public ResponseEntity<?> openUploadsFolder() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nautilus", "--new-window", UPLOAD_DIR);
            pb.environment().put("DISPLAY", ":0");
            pb.start();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            try {
                ProcessBuilder pb2 = new ProcessBuilder("xdg-open", UPLOAD_DIR);
                pb2.environment().put("DISPLAY", ":0");
                pb2.start();
                return ResponseEntity.ok().build();
            } catch (Exception ex) {
                return ResponseEntity.status(500).body("Failed to open folder: " + ex.getMessage());
            }
        }
    }

    public static class FileInfo {
        private final String name;
        private final String extension;
        private final long size;
        private final long timestamp;

        public FileInfo(String name, String extension, long size, long timestamp) {
            this.name = name;
            this.extension = extension;
            this.size = size;
            this.timestamp = timestamp;
        }

        public String getName() {
            return name;
        }

        public String getExtension() {
            return extension;
        }

        public long getSize() {
            return size;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
