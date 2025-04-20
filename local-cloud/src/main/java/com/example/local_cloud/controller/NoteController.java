package com.example.local_cloud.controller;

import com.example.local_cloud.model.Note;
import com.example.local_cloud.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        model.addAttribute("note", note);
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
            return "note_form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Error: Note not found or corrupted");
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
            if (note.getTitle() != null && !note.getTitle().isEmpty()) {
                // Check if the note already exists by ID
                if (note.getId() != null && !note.getId().isEmpty()) {
                    // Try to find the existing note first to maintain the same folder
                    try {
                        Note existingNote = noteService.loadNote(note.getSpace(), note.getCategory(), note.getId());
                        if (existingNote != null && existingNote.getFolderName() != null) {
                            // Reuse the existing folder name
                            note.setFolderName(existingNote.getFolderName());
                            logger.info("Auto-save: Reusing existing note folder: {}", existingNote.getFolderName());
                        }
                    } catch (Exception e) {
                        logger.warn("Could not find existing note for auto-save: {}", e.getMessage());
                        // Continue with saving as new note would happen below
                    }
                }
                
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
                                        @RequestParam("upload") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Received file upload request for noteId: {}, file name: {}, size: {}", 
                noteId, file.getOriginalFilename(), file.getSize());
            
            // Get the note to determine its folder
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                logger.error("Note not found or folder name is null for noteId: {}", noteId);
                response.put("success", false);
                response.put("error", "Note not found or not saved yet");
                return response;
            }
            
            // Create attachments directory if it doesn't exist
            Path attachmentsDir = Paths.get(basePath, space, category, note.getFolderName(), "attachments");
            Files.createDirectories(attachmentsDir);
            logger.info("Attachments directory: {}", attachmentsDir);
            
            // Save the file
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "file_" + System.currentTimeMillis();
            }
            
            Path filePath = attachmentsDir.resolve(originalFilename);
            
            // If file with same name exists, rename
            int count = 1;
            String fileBaseName = originalFilename;
            String extension = "";
            
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                fileBaseName = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex);
            }
            
            while (Files.exists(filePath)) {
                String newName = fileBaseName + "_" + count + extension;
                filePath = attachmentsDir.resolve(newName);
                count++;
            }
            
            // Ensure the file is saved properly
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
            
            // Log the file path to help debug
            logger.info("File saved to: {}", filePath.toAbsolutePath());
            
            // Calculate the URL - important for accessing the file later
            String fileName = filePath.getFileName().toString();
            String url = "/uploads/" + space + "/" + category + "/" + note.getFolderName() + "/attachments/" + fileName;
            logger.info("Generated URL for file: {}", url);
            
            response.put("success", true);
            response.put("url", url);
            response.put("filename", fileName);
            
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
            return "note_view";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("toastMessage", "❌ Error: Note not found or corrupted");
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
        List<Map<String, String>> attachments = new ArrayList<>();
        
        try {
            // Get the note to determine its folder
            Note note = noteService.loadNote(space, category, noteId);
            if (note == null || note.getFolderName() == null) {
                response.put("success", false);
                response.put("error", "Note not found");
                return response;
            }
            
            // Look for attachments directory
            Path attachmentsDir = Paths.get(basePath, space, category, note.getFolderName(), "attachments");
            if (Files.exists(attachmentsDir)) {
                // List all files in the attachments directory
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(attachmentsDir)) {
                    for (Path file : stream) {
                        if (Files.isRegularFile(file)) {
                            String fileName = file.getFileName().toString();
                            String url = "/uploads/" + space + "/" + category + "/" + note.getFolderName() + "/attachments/" + fileName;
                            String fileType = Files.probeContentType(file);
                            boolean isImage = fileType != null && fileType.startsWith("image/");
                            
                            Map<String, String> fileInfo = new HashMap<>();
                            fileInfo.put("name", fileName);
                            fileInfo.put("url", url);
                            fileInfo.put("type", fileType != null ? fileType : "application/octet-stream");
                            fileInfo.put("isImage", String.valueOf(isImage));
                            fileInfo.put("size", String.valueOf(Files.size(file)));
                            
                            attachments.add(fileInfo);
                        }
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
}
