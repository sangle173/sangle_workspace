package com.example.local_cloud.service;
import com.example.local_cloud.model.Note;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NoteService {

    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);

    @Value("${notes.base-path}")
    private String basePath;

    private final ObjectMapper mapper;

    public NoteService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<Note> listNotes(String space, String category) throws IOException {
        Path folder = getPath(space, category);
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
            return Collections.emptyList();
        }

        List<Note> notes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path noteDir : stream) {
                Path jsonFile = noteDir.resolve("note.json");
                if (Files.exists(jsonFile)) {
                    try {
                        Note note = mapper.readValue(jsonFile.toFile(), Note.class);
                        note.setFolderName(noteDir.getFileName().toString());
                        notes.add(note);
                    } catch (Exception e) {
                        logger.error("Error reading note file: {}", jsonFile, e);
                        handleCorruptedFile(jsonFile);
                    }
                }
            }
        }
        
        // Sort notes by updatedAt date in descending order
        notes.sort((a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
            if (a.getUpdatedAt() == null) return 1;
            if (b.getUpdatedAt() == null) return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });
        
        return notes;
    }

    private Path getPath(String... parts) {
        Path base = Paths.get(basePath);
        if (parts == null || parts.length == 0) {
            return base;
        }
        return base.resolve(Paths.get("", parts));
    }

    public Note loadNote(String space, String category, String noteId) throws IOException {
        if (space == null || category == null || noteId == null) {
            throw new IllegalArgumentException("Space, category, and noteId must not be null");
        }

        try {
            // First try to find by ID
            List<Note> notes = listNotes(space, category);
            Optional<Note> foundNote = notes.stream()
                    .filter(note -> noteId.equals(note.getId()))
                    .findFirst();
            
            if (foundNote.isPresent()) {
                return foundNote.get();
            }
            
            // Try using noteId as folder name
            Path noteDir = getPath(space, category, noteId);
            Path jsonFile = noteDir.resolve("note.json");
            
            if (!Files.exists(noteDir) || !Files.exists(jsonFile)) {
                throw new IOException("Note not found: " + noteId);
            }
            
            Note note = mapper.readValue(jsonFile.toFile(), Note.class);
            
            // Initialize empty content if null
            if (note.getContentHtml() == null) {
                note.setContentHtml("");
            }
            
            note.setFolderName(noteDir.getFileName().toString());
            return note;
            
        } catch (Exception e) {
            logger.error("Failed to load note - space: {}, category: {}, noteId: {}", space, category, noteId, e);
            throw new IOException("Could not load note: " + e.getMessage(), e);
        }
    }

    public void deleteNote(String space, String category, String noteId) throws IOException {
        Note note = null;
        try {
            note = loadNote(space, category, noteId);
        } catch (IOException e) {
            // Try direct folder approach
            Path noteDir = getPath(space, category, noteId);
            if (Files.exists(noteDir)) {
                deleteDirectory(noteDir);
                return;
            }
            throw e;
        }
        
        if (note != null && note.getFolderName() != null) {
            Path noteDir = getPath(space, category, note.getFolderName());
            if (Files.exists(noteDir)) {
                deleteDirectory(noteDir);
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    public void saveNote(Note note) throws IOException {
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (note.getCreatedAt() == null) {
            note.setCreatedAt(now);
        }
        note.setUpdatedAt(now);

        // Generate folder name for new notes
        if (note.getFolderName() == null || note.getFolderName().isEmpty()) {
            String prefix = "d";  // Default prefix for notes
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            note.setFolderName(prefix + "_" + timestamp);
            logger.info("Generated new folder name: {}", note.getFolderName());
        }
        
        // Ensure note has an ID
        if (note.getId() == null || note.getId().isEmpty()) {
            note.setId(UUID.randomUUID().toString());
            logger.info("Generated new ID for note: {}", note.getId());
        }
        
        // Create folder and save note
        Path noteFolder = getPath(note.getSpace(), note.getCategory(), note.getFolderName());
        Files.createDirectories(noteFolder);

        // Create attachments folder if it doesn't exist
        Path attachmentsFolder = noteFolder.resolve("attachments");
        if (!Files.exists(attachmentsFolder)) {
            Files.createDirectories(attachmentsFolder);
        }

        Path jsonPath = noteFolder.resolve("note.json");
        mapper.writeValue(jsonPath.toFile(), note);
        logger.info("Saved note to: {}", jsonPath);
    }

    public boolean isValidNoteJson(Path jsonFile) {
        try {
            if (Files.exists(jsonFile)) {
                mapper.readValue(jsonFile.toFile(), Note.class);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Invalid JSON file: {}", jsonFile, e);
            return false;
        }
    }

    public boolean handleCorruptedFile(Path jsonFile) {
        try {
            logger.warn("Attempting to handle corrupted file: {}", jsonFile);
            Files.deleteIfExists(jsonFile);
            
            Path parent = jsonFile.getParent();
            if (parent != null && Files.exists(parent)) {
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(parent)) {
                    if (!dirStream.iterator().hasNext()) {
                        Files.deleteIfExists(parent);
                        logger.info("Deleted empty note directory: {}", parent);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to handle corrupted file: {}", jsonFile, e);
            return false;
        }
    }

    public void saveAttachment(String space, String category, String noteId, MultipartFile file) throws IOException {
        Note note = loadNote(space, category, noteId);
        if (note == null || note.getFolderName() == null) {
            throw new IOException("Note not found");
        }

        Path attachmentsDir = getPath(space, category, note.getFolderName(), "attachments");
        Files.createDirectories(attachmentsDir);

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

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public List<Map<String, Object>> getAttachments(String space, String category, String noteId) throws IOException {
        Note note = loadNote(space, category, noteId);
        if (note == null || note.getFolderName() == null) {
            throw new IOException("Note not found");
        }

        Path attachmentsDir = getPath(space, category, note.getFolderName(), "attachments");
        if (!Files.exists(attachmentsDir)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> attachments = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(attachmentsDir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String fileName = file.getFileName().toString();
                    String contentType = Files.probeContentType(file);
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", fileName);
                    fileInfo.put("url", String.format("/uploads/%s/%s/%s/attachments/%s", 
                        space, category, note.getFolderName(), fileName));
                    fileInfo.put("type", contentType);
                    fileInfo.put("size", Files.size(file));
                    fileInfo.put("isImage", contentType != null && contentType.startsWith("image/"));
                    attachments.add(fileInfo);
                }
            }
        }
        return attachments;
    }
}
