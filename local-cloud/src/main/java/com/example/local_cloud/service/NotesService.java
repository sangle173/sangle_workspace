package com.example.local_cloud.service;

import com.example.local_cloud.model.Note;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotesService {
    private static final Logger logger = LoggerFactory.getLogger(NotesService.class);
    private final Path notesDir;
    private final Path imagesDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotesService() {
        // Use Desktop/LocalNotes as the base directory
        this.notesDir = Paths.get(System.getProperty("user.home"), "Desktop", "LocalNotes");
        this.imagesDir = notesDir.resolve("images");
        try {
            if (!Files.exists(notesDir)) {
                Files.createDirectories(notesDir);
                logger.info("Created notes directory at: {}", notesDir);
            }
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
                logger.info("Created images directory at: {}", imagesDir);
            }
        } catch (IOException e) {
            logger.error("Failed to create directories", e);
            throw new RuntimeException("Failed to create directories", e);
        }
    }

    public String getBaseDir() {
        return notesDir.toString();
    }

    public List<String> getFolders() throws IOException {
        logger.info("Getting folders from: {}", notesDir);
        return Files.list(notesDir)
                .filter(Files::isDirectory)
                .filter(path -> !path.equals(imagesDir))
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    public void createFolder(String folderName) throws IOException {
        Path folderPath = notesDir.resolve(folderName);
        if (!Files.exists(folderPath)) {
            Files.createDirectory(folderPath);
            logger.info("Created folder: {}", folderPath);
        } else {
            logger.info("Folder already exists: {}", folderPath);
        }
    }

    public List<Note> getNotesInFolder(String folder) throws IOException {
        Path folderPath = notesDir.resolve(folder);
        if (!Files.exists(folderPath)) {
            logger.warn("Folder does not exist: {}", folderPath);
            return new ArrayList<>();
        }

        return Files.list(folderPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .map(path -> {
                    try {
                        String content = Files.readString(path);
                        Note note = new Note();
                        note.setId(path.getFileName().toString().replace(".txt", ""));
                        note.setTitle(content.split("\n")[0]);
                        note.setContent(content);
                        note.setFolder(folder);
                        return note;
                    } catch (IOException e) {
                        logger.error("Failed to read note: {}", path, e);
                        throw new RuntimeException("Failed to read note: " + path, e);
                    }
                })
                .collect(Collectors.toList());
    }

    public Note getNote(String folder, String noteId) throws IOException {
        Path notePath = notesDir.resolve(folder).resolve(noteId + ".txt");
        if (!Files.exists(notePath)) {
            logger.warn("Note does not exist: {}", notePath);
            return null;
        }

        String content = Files.readString(notePath);
        Note note = new Note();
        note.setId(noteId);
        note.setTitle(content.split("\n")[0]);
        note.setContent(content);
        note.setFolder(folder);
        return note;
    }

    public void saveNote(Note note) throws IOException {
        Path folderPath = notesDir.resolve(note.getFolder());
        if (!Files.exists(folderPath)) {
            Files.createDirectory(folderPath);
            logger.info("Created folder for note: {}", folderPath);
        }

        // If this is a new note (no ID), generate a new ID
        if (note.getId() == null) {
            note.setId(UUID.randomUUID().toString());
            logger.info("Generated new ID for note: {}", note.getId());
        }

        Path notePath = folderPath.resolve(note.getId() + ".txt");
        note.setUpdatedAt(LocalDateTime.now());
        
        // Format the content with title and content
        String formattedContent = note.getTitle() + "\n" + note.getContent();
        Files.writeString(notePath, formattedContent);
        logger.info("Saved note: {}", notePath);
    }

    public void deleteNote(String folder, String noteId) throws IOException {
        Path notePath = notesDir.resolve(folder).resolve(noteId + ".txt");
        if (Files.exists(notePath)) {
            Files.delete(notePath);
            logger.info("Deleted note: {}", notePath);
        } else {
            logger.warn("Note does not exist: {}", notePath);
        }
    }

    public Map<String, Object> saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path imagePath = imagesDir.resolve(fileName);
        Files.copy(file.getInputStream(), imagePath);
        logger.info("Saved image: {}", imagePath);

        Map<String, Object> response = new HashMap<>();
        response.put("uploaded", true);
        response.put("url", "/api/notes/images/" + fileName);
        return response;
    }

    public byte[] getImage(String fileName) throws IOException {
        Path imagePath = imagesDir.resolve(fileName);
        if (!Files.exists(imagePath)) {
            logger.warn("Image does not exist: {}", imagePath);
            return null;
        }
        return Files.readAllBytes(imagePath);
    }
} 