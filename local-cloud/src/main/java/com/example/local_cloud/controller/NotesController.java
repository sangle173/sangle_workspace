package com.example.local_cloud.controller;

import com.example.local_cloud.model.Note;
import com.example.local_cloud.service.NotesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;

@Controller
public class NotesController {

    @Autowired
    private NotesService notesService;

    @GetMapping("/notes")
    public String notesPage() {
        return "notes";
    }

    @GetMapping("/view/{folder}/{noteId}")
    public String viewNote(@PathVariable String folder, @PathVariable String noteId, Model model) {
        try {
            Note note = notesService.getNote(folder, noteId);
            if (note == null) {
                return "redirect:/notes?error=Note+not+found";
            }
            
            // Process content to remove title from the beginning
            String content = note.getContent();
            String title = note.getTitle();
            
            // Escape special regex characters in the title
            String titleEscaped = title.replace("\\", "\\\\")
                                       .replace(".", "\\.")
                                       .replace("*", "\\*")
                                       .replace("+", "\\+")
                                       .replace("?", "\\?")
                                       .replace("^", "\\^")
                                       .replace("$", "\\$")
                                       .replace("(", "\\(")
                                       .replace(")", "\\)")
                                       .replace("[", "\\[")
                                       .replace("]", "\\]")
                                       .replace("{", "\\{")
                                       .replace("}", "\\}")
                                       .replace("|", "\\|");
            
            // Create a regex to match the title if it appears at the beginning
            // This handles various formats like plain text or HTML headings
            String titleRegex = "^\\s*(?:<(?:h[1-6]|p|div)[^>]*>" + titleEscaped + "</(?:h[1-6]|p|div)>|" + titleEscaped + "(?:<br>|\\s*))\\s*";
            
            // Remove the title from the beginning of content
            String processedContent = content.replaceFirst(titleRegex, "");
            
            model.addAttribute("note", note);
            model.addAttribute("processedContent", processedContent);
            return "view-note";
        } catch (IOException e) {
            return "redirect:/notes?error=Error+loading+note";
        }
    }

    @GetMapping("/api/notes/folders")
    @ResponseBody
    public ResponseEntity<List<String>> getFolders() {
        try {
            return ResponseEntity.ok(notesService.getFolders());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/notes/folders")
    @ResponseBody
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request) {
        try {
            String folderName = request.get("name");
            if (folderName == null || folderName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Folder name cannot be empty");
            }
            notesService.createFolder(folderName.trim());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/notes/{folder}")
    @ResponseBody
    public ResponseEntity<List<Note>> getNotes(@PathVariable String folder) {
        try {
            return ResponseEntity.ok(notesService.getNotesInFolder(folder));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/notes/{folder}/{noteId}")
    @ResponseBody
    public ResponseEntity<Note> getNote(@PathVariable String folder, @PathVariable String noteId) {
        try {
            Note note = notesService.getNote(folder, noteId);
            if (note == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(note);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/notes/save")
    @ResponseBody
    public ResponseEntity<?> saveNote(@RequestBody Note note) {
        try {
            if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title cannot be empty");
            }
            if (note.getFolder() == null || note.getFolder().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Folder cannot be empty");
            }
            if (note.getContent() == null) {
                note.setContent("");
            }
            notesService.saveNote(note);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/api/notes/{folder}/{noteId}")
    @ResponseBody
    public ResponseEntity<?> deleteNote(@PathVariable String folder, @PathVariable String noteId) {
        try {
            notesService.deleteNote(folder, noteId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/notes/upload-image")
    @ResponseBody
    public ResponseEntity<?> uploadImage(@RequestParam("upload") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file uploaded");
            }
            Map<String, Object> response = notesService.saveImage(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/notes/images/{fileName}")
    @ResponseBody
    public ResponseEntity<?> getImage(@PathVariable String fileName) {
        try {
            byte[] imageBytes = notesService.getImage(fileName);
            if (imageBytes == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 