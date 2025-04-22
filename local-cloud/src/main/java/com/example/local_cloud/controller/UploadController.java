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
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "convert", defaultValue = "false") boolean convert,
            @RequestParam(value = "quality", defaultValue = "720") String quality) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.status(400).body("File name cannot be null");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            boolean shouldConvert = convert || extension.equals("mov");

            Path dest = shouldConvert
                    ? Paths.get(UPLOAD_DIR, baseName + ".mp4")
                    : Paths.get(UPLOAD_DIR, originalFilename);

            if (shouldConvert && (extension.equals("mp4") || extension.equals("mov") || extension.equals("webm"))) {
                Path tempFile = Paths.get(UPLOAD_DIR, "temp_" + originalFilename);
                Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                List<String> command = Arrays.asList(
                        "ffmpeg", "-i", tempFile.toString(),
                        "-vf", "scale=-2:" + quality,
                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-crf", "28",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-movflags", "+faststart",
                        dest.toString());

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[FFmpeg] " + line);
                    }
                }

                int exitCode = process.waitFor();
                Files.deleteIfExists(tempFile);

                if (exitCode != 0) {
                    return ResponseEntity.status(500).body("FFmpeg conversion failed.");
                }

                return ResponseEntity.ok("Converted and uploaded: " + dest.getFileName());
            } else {
                Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                return ResponseEntity.ok("Uploaded: " + originalFilename);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-chunk")
    public ResponseEntity<String> uploadChunk(@RequestParam("chunk") MultipartFile chunk,
            @RequestParam("filename") String filename,
            @RequestParam("chunkIndex") int index,
            @RequestParam(value = "quality", defaultValue = "720") String quality) {
        try {
            Path chunkFolder = Paths.get(TEMP_DIR, filename);
            Files.createDirectories(chunkFolder);
            Path chunkPath = chunkFolder.resolve("chunk" + index);
            Files.copy(chunk.getInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok("Chunk " + index + " received");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Chunk upload failed");
        }
    }

    @PostMapping("/merge-chunks")
    public ResponseEntity<String> mergeChunks(@RequestBody Map<String, Object> body) {
        String filename = (String) body.get("filename");
        int totalChunks = (int) body.get("totalChunks");
        boolean convert = body.get("convert") != null && (boolean) body.get("convert");
        String quality = body.get("quality") != null ? (String) body.get("quality") : "720";

        Path chunkFolder = Paths.get(TEMP_DIR, filename);
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String baseName = filename.substring(0, filename.lastIndexOf('.'));
        boolean shouldConvert = convert || extension.equals("mov");

        Path mergedFile = shouldConvert
                ? Paths.get(UPLOAD_DIR, "temp_" + filename)
                : Paths.get(UPLOAD_DIR, filename);

        try (OutputStream out = Files.newOutputStream(mergedFile)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunk = chunkFolder.resolve("chunk" + i);
                Files.copy(chunk, out);
                Files.delete(chunk);
            }
            Files.delete(chunkFolder);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Merge failed: " + e.getMessage());
        }

        if (shouldConvert && (extension.equals("mp4") || extension.equals("mov") || extension.equals("webm"))) {
            try {
                Path output = Paths.get(UPLOAD_DIR, baseName + ".mp4");

                List<String> command = Arrays.asList(
                        "ffmpeg", "-i", mergedFile.toString(),
                        "-vf", "scale=-2:" + quality,
                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-crf", "28",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-movflags", "+faststart",
                        output.toString());

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[FFmpeg] " + line);
                    }
                }

                int exitCode = process.waitFor();
                Files.deleteIfExists(mergedFile);

                if (exitCode != 0) {
                    return ResponseEntity.status(500).body("FFmpeg conversion failed after merge.");
                }

                return ResponseEntity.ok("Merged and converted: " + baseName + ".mp4");
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Conversion error: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("File merged: " + filename);
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
