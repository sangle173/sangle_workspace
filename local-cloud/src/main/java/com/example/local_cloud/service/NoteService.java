package com.example.local_cloud.service;
import com.example.local_cloud.model.Note;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File; // ✅ add this line
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
        Path folder = Paths.get(basePath, space, category);
        if (!Files.exists(folder)) return Collections.emptyList();

        List<Note> notes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path noteDir : stream) {
                Path jsonFile = noteDir.resolve("note.json");
                if (Files.exists(jsonFile)) {
                    try {
                        Note note = mapper.readValue(jsonFile.toFile(), Note.class);
                        // Set the folder name so we can use it later
                        note.setFolderName(noteDir.getFileName().toString());
                        notes.add(note);
                    } catch (Exception e) {
                        logger.error("Error reading note file: {}", jsonFile, e);
                        // Try to handle the corrupted file
                        handleCorruptedFile(jsonFile);
                    }
                }
            }
        }
        
        // Sort notes by updatedAt date in descending order (most recent first)
        notes.sort((a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
            if (a.getUpdatedAt() == null) return 1;
            if (b.getUpdatedAt() == null) return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });
        
        return notes;
    }

    public Note loadNote(String space, String category, String noteId) throws IOException {
        // First try to find the note by ID by listing all notes
        for (Note note : listNotes(space, category)) {
            if (note.getId().equals(noteId)) {
                return note;
            }
        }
        
        // If not found by ID, try using noteId as folder name directly
        Path noteDir = Paths.get(basePath, space, category, noteId);
        Path jsonFile = noteDir.resolve("note.json");
        
        // Check if directory and file exist
        if (!Files.exists(noteDir) || !Files.exists(jsonFile)) {
            logger.error("Note file not found: {}", jsonFile);
            throw new IOException("Note not found: " + noteId);
        }
        
        try {
            Note note = mapper.readValue(jsonFile.toFile(), Note.class);
            note.setFolderName(noteDir.getFileName().toString());
            return note;
        } catch (Exception e) {
            logger.error("Failed to load note file: {}", jsonFile, e);
            throw new IOException("Could not load note: " + e.getMessage(), e);
        }
    }

    public void deleteNote(String space, String category, String noteId) throws IOException {
        // First try to find the note by ID
        Note note = null;
        try {
            note = loadNote(space, category, noteId);
        } catch (IOException e) {
            // If not found by ID, try direct folder approach
            Path noteDir = Paths.get(basePath, space, category, noteId);
            if (Files.exists(noteDir)) {
                Files.walk(noteDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                return;
            }
            throw e;
        }
        
        // If found by ID, delete using the folder name
        if (note != null && note.getFolderName() != null) {
            Path noteDir = Paths.get(basePath, space, category, note.getFolderName());
            if (Files.exists(noteDir)) {
                Files.walk(noteDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }

    /**
     * Handles corrupted note files by attempting to delete them
     * @param jsonFile Path to the corrupted JSON file
     * @return true if the file was successfully handled, false otherwise
     */
    public boolean handleCorruptedFile(Path jsonFile) {
        try {
            logger.warn("Attempting to delete corrupted note file: {}", jsonFile);
            Files.deleteIfExists(jsonFile);
            // If the file is in a directory with no other files, delete the directory too
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

    public void saveNote(Note note) throws IOException {
        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }
        if (note.getCreatedAt() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }

        // Use existing folder name if available to avoid creating duplicate notes
        String folderName;
        if (note.getFolderName() != null && !note.getFolderName().isEmpty()) {
            folderName = note.getFolderName();
            logger.info("Reusing existing folder: {}", folderName);
        } else {
            // Create a new folder name for new notes
            String safeTitle = note.getTitle().replaceAll("[^a-zA-Z0-9\\-_]", "_");
            folderName = safeTitle + "_" + note.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            logger.info("Creating new folder: {}", folderName);
        }
        
        note.setFolderName(folderName);
        
        Path noteFolder = Paths.get(basePath, note.getSpace(), note.getCategory(), folderName);
        Files.createDirectories(noteFolder);

        Path jsonPath = noteFolder.resolve("note.json");
        mapper.writeValue(jsonPath.toFile(), note);
    }

    /**
     * Tests if a JSON file can be parsed as a Note
     * @param jsonFile The path to the JSON file
     * @return true if valid, false if invalid
     */
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
}
