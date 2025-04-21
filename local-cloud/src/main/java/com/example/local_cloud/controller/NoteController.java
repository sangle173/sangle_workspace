package com.example.local_cloud.controller;

import com.example.local_cloud.model.Note;
import com.example.local_cloud.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/spaces/{space}/categories/{category}/notes")
public class NoteController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    @Autowired
    private NoteService noteService;
    
    @Value("${notes.base-path}")
    private String basePath;

    @GetMapping
    public String listNotes(@PathVariable String space,
                            @PathVariable String category,
                            Model model) throws IOException {
        List<Note> notes = noteService.listNotes(space, category);
        model.addAttribute("notes", notes);
        model.addAttribute("space", space);
        model.addAttribute("category", category);
        return "note_list";
    }

    @GetMapping("/new")
    public String newNote(@PathVariable String space,
                          @PathVariable String category,
                          Model model) {
        Note note = new Note();
        note.setSpace(space);
        note.setCategory(category);
        note.setId(null); // Ensure ID is null for new notes
        model.addAttribute("note", note);
        model.addAttribute("isNewNote", true); // Add flag to distinguish new notes
        return "note_form";
    }

    @GetMapping("/{noteId}/edit")
    public String editNote(@PathVariable String space,
                           @PathVariable String category,
                           @PathVariable String noteId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Note note = noteService.loadNote(space, category, noteId);
            model.addAttribute("note", note);
            model.addAttribute("isNewNote", false);
            return "note_form";
        } catch (Exception e) {
            logger.error("Error loading note for editing: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "danger");
            return "redirect:/spaces/" + space + "/categories/" + category + "/notes";
        }
    }

    @PostMapping("/save")
    public String saveNote(@ModelAttribute Note note,
                           RedirectAttributes redirectAttributes) throws IOException {
        try {
            noteService.saveNote(note);
            redirectAttributes.addFlashAttribute("toastMessage", "✅ Note saved");
            redirectAttributes.addFlashAttribute("toastType", "success");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Error saving note: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "danger");
            throw e;
        }
        return "redirect:/spaces/" + note.getSpace() + "/categories/" + note.getCategory() + "/notes";
    }
    
    @PostMapping("/autosave")
    @ResponseBody
    public Map<String, Object> autoSaveNote(@RequestBody Note note) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Only save if title is not empty
            if (note.getTitle() != null && !note.getTitle().trim().isEmpty()) {
                // Generate new ID if not exists
                if (note.getId() == null || note.getId().isEmpty()) {
                    note.setId(UUID.randomUUID().toString());
                    logger.info("Generated new ID for note: {}", note.getId());
                }
                
                // Set timestamps if not set
                if (note.getCreatedAt() == null) {
                    note.setCreatedAt(LocalDateTime.now());
                }
                note.setUpdatedAt(LocalDateTime.now());
                
                // Save the note
                noteService.saveNote(note);
                
                response.put("success", true);
                response.put("id", note.getId());
                response.put("folderName", note.getFolderName());
                logger.info("Auto-saved note with ID: {} and folder: {}", note.getId(), note.getFolderName());
            } else {
                response.put("success", false);
                response.put("error", "Title is required");
            }
        } catch (Exception e) {
            logger.error("Error during auto-save", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/{noteId}/upload")
    @ResponseBody
    public Map<String, Object> uploadFile(@PathVariable String space,
                                        @PathVariable String category,
                                        @PathVariable String noteId,
                                        @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        logger.info("Starting file upload - space: {}, category: {}, noteId: {}, filename: {}", 
            space, category, noteId, file.getOriginalFilename());
        
        try {
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null) {
                logger.error("Note not found - noteId: {}", noteId);
                response.put("success", false);
                response.put("error", "Note not found");
                return response;
            }

            if (note.getFolderName() == null) {
                logger.error("Note folder name is null - noteId: {}", noteId);
                response.put("success", false);
                response.put("error", "Note folder not initialized");
                return response;
            }

            // Create attachments directory
            Path attachmentsDir = Paths.get(basePath, space, category, note.getFolderName(), "attachments");
            logger.info("Creating attachments directory: {}", attachmentsDir);
            Files.createDirectories(attachmentsDir);

            // Process the file name
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
            logger.info("Saving file to: {}", filePath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Generate URL for accessing the file
            String url = String.format("/uploads/%s/%s/%s/attachments/%s", 
                space, category, note.getFolderName(), safeFilename);
            
            String contentType = file.getContentType();
            response.put("success", true);
            response.put("url", url);
            response.put("filename", safeFilename);
            response.put("type", contentType);
            response.put("size", file.getSize());
            response.put("isImage", contentType != null && contentType.startsWith("image/"));

        } catch (Exception e) {
            logger.error("Error uploading file", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @PostMapping("/{noteId}/delete")
    public String deleteNote(@PathVariable String space,
                             @PathVariable String category,
                             @PathVariable String noteId,
                             RedirectAttributes redirectAttributes) {
        try {
            noteService.deleteNote(space, category, noteId);
            redirectAttributes.addFlashAttribute("toastMessage", "🗑️ Note deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Failed to delete: " + e.getMessage());
        }
        return "redirect:/spaces/" + space + "/categories/" + category + "/notes";
    }

    @GetMapping("/{noteId}")
    public String viewNote(@PathVariable String space,
                           @PathVariable String category,
                           @PathVariable String noteId,
                           Model model,
                           RedirectAttributes redirectAttributes) throws IOException {
        try {
            Note note = noteService.loadNote(space, category, noteId);
            model.addAttribute("note", note);
            model.addAttribute("space", space);
            model.addAttribute("category", category);
            return "note_view";
        } catch (IOException e) {
            logger.error("Error loading note: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "danger");
            return "redirect:/spaces/" + space + "/categories/" + category + "/notes";
        }
    }

    @GetMapping("/maintenance/repair")
    public String repairCorruptedNotes(@PathVariable String space,
                                      @PathVariable String category,
                                      RedirectAttributes redirectAttributes) {
        AtomicInteger repairedCount = new AtomicInteger(0);
        try {
            Path folder = Paths.get(basePath, space, category);
            if (Files.exists(folder)) {
                Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().equals("note.json")) {
                            // Test if the JSON file is valid
                            if (!noteService.isValidNoteJson(file)) {
                                // If invalid, handle the corrupted file
                                if (noteService.handleCorruptedFile(file)) {
                                    repairedCount.incrementAndGet();
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            redirectAttributes.addFlashAttribute("toastMessage", "🔧 Repaired " + repairedCount.get() + " corrupted notes");
            redirectAttributes.addFlashAttribute("toastType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Error during repair: " + e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "danger");
        }
        return "redirect:/spaces/" + space + "/categories/" + category + "/notes";
    }

    @GetMapping("/{noteId}/attachments")
    @ResponseBody
    public Map<String, Object> getAttachments(@PathVariable String space,
                                            @PathVariable String category,
                                            @PathVariable String noteId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                response.put("success", false);
                response.put("error", "Note not found");
                return response;
            }

            Path attachmentsDir = Paths.get(basePath, space, category, note.getFolderName(), "attachments");
            if (!Files.exists(attachmentsDir)) {
                response.put("success", true);
                response.put("attachments", new ArrayList<>());
                return response;
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

            response.put("success", true);
            response.put("attachments", attachments);

        } catch (Exception e) {
            logger.error("Error getting attachments", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{noteId}/attachments/{fileName}")
    @ResponseBody
    public Map<String, Object> deleteAttachment(@PathVariable String space,
                                              @PathVariable String category,
                                              @PathVariable String noteId,
                                              @PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                response.put("success", false);
                response.put("error", "Note not found");
                return response;
            }

            Path attachmentPath = Paths.get(basePath, space, category, note.getFolderName(), "attachments", fileName);
            if (Files.exists(attachmentPath)) {
                Files.delete(attachmentPath);
                response.put("success", true);
                response.put("message", "File deleted successfully");
            } else {
                response.put("success", false);
                response.put("error", "File not found");
            }

        } catch (Exception e) {
            logger.error("Error deleting attachment", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @GetMapping("/file-info")
    @ResponseBody
    public Map<String, Object> getFileTypeInfo(@RequestParam String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            String contentType = Files.probeContentType(Paths.get(fileName));
            response.put("success", true);
            response.put("contentType", contentType);
            response.put("isImage", contentType != null && contentType.startsWith("image/"));
            response.put("isVideo", contentType != null && contentType.startsWith("video/"));
            response.put("isAudio", contentType != null && contentType.startsWith("audio/"));
            response.put("isPdf", contentType != null && contentType.equals("application/pdf"));
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    protected void finalize() {
        // Clean up temporary files when the application shuts down
        try {
            Path tempDir = Paths.get(basePath);
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .filter(path -> Files.isRegularFile(path))
                    .forEach(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            // Delete files older than 24 hours that haven't been accessed
                            if (attrs.lastAccessTime().toInstant().plus(24, ChronoUnit.HOURS)
                                    .isBefore(Instant.now())) {
                                Files.deleteIfExists(path);
                            }
                        } catch (IOException e) {
                            logger.error("Error cleaning up file: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            logger.error("Error during cleanup", e);
        }
    }
}
