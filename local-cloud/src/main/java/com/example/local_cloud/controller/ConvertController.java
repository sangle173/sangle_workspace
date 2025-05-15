package com.example.local_cloud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class ConvertController {
    private static final String DESKTOP = System.getProperty("user.home") + File.separator + "Desktop";
    private static final String UPLOAD_DIR = DESKTOP + File.separator + "LocalCloudUploads";
    private static final String CONVERT_DIR = DESKTOP + File.separator + "ConvertFolder";

    // Task tracking for progress/log
    private static final Map<String, ConvertTask> tasks = new ConcurrentHashMap<>();

    static {
        try {
            Files.createDirectories(Paths.get(CONVERT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/convert-video")
    @ResponseBody
    public Map<String, Object> convertVideo(@RequestBody Map<String, String> body) {
        Map<String, Object> resp = new HashMap<>();
        String filename = body.get("filename");
        boolean removeAudio = false;
        if (body.containsKey("removeAudio")) {
            Object val = body.get("removeAudio");
            if (val instanceof Boolean) removeAudio = (Boolean) val;
            else if (val instanceof String) removeAudio = Boolean.parseBoolean((String) val);
        }
        if (filename == null || filename.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "No filename provided");
            return resp;
        }
        Path input = Paths.get(UPLOAD_DIR, filename);
        if (!Files.exists(input)) {
            resp.put("success", false);
            resp.put("message", "File not found");
            return resp;
        }
        String baseName = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
        Path output = Paths.get(CONVERT_DIR, baseName + ".mp4");
        String taskId = UUID.randomUUID().toString();
        ConvertTask task = new ConvertTask();
        tasks.put(taskId, task);
        List<String> cmd = new java.util.ArrayList<>(java.util.Arrays.asList(
            "HandBrakeCLI",
            "-i", input.toString(),
            "-o", output.toString(),
            "-e", "x264",
            "-q", "28",
            "--optimize",
            "--format", "av_mp4",
            "--width", "640"
        ));
        if (removeAudio) {
            cmd.add("--audio");
            cmd.add("none");
        } else {
            cmd.addAll(java.util.Arrays.asList(
                "--audio", "1",
                "--aencoder", "av_aac",
                "--ab", "128"
            ));
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        new Thread(() -> {
            try {
                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[HandBrakeCLI] " + line);
                        task.appendLog(line + "\n");
                        // Try to parse progress from HandBrakeCLI output
                        int percent = parseHandbrakeProgress(line);
                        if (percent >= 0) {
                            task.setProgress(percent);
                        }
                    }
                }
                proc.waitFor();
                task.setProgress(100);
                task.setDone(true);
            } catch (Exception e) {
                task.appendLog("[ERROR] " + e.getMessage() + "\n");
                task.setDone(true);
            }
        }).start();
        resp.put("success", true);
        resp.put("taskId", taskId);
        return resp;
    }

    @GetMapping("/convert-progress")
    @ResponseBody
    public Map<String, Object> convertProgress(@RequestParam String taskId) {
        Map<String, Object> resp = new HashMap<>();
        ConvertTask task = tasks.get(taskId);
        if (task == null) {
            resp.put("progress", 0);
            resp.put("log", "No such task");
            resp.put("done", true);
            return resp;
        }
        resp.put("progress", task.getProgress());
        resp.put("log", task.getLog());
        resp.put("done", task.isDone());
        if (task.isDone()) {
            tasks.remove(taskId);
        }
        return resp;
    }

    @PostMapping("/open-convertfolder")
    @ResponseBody
    public ResponseEntity<?> openConvertFolder() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nautilus", "--new-window", CONVERT_DIR);
            pb.environment().put("DISPLAY", ":0");
            pb.start();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            try {
                ProcessBuilder pb2 = new ProcessBuilder("xdg-open", CONVERT_DIR);
                pb2.environment().put("DISPLAY", ":0");
                pb2.start();
                return ResponseEntity.ok().build();
            } catch (Exception ex) {
                return ResponseEntity.status(500).body("Failed to open folder: " + ex.getMessage());
            }
        }
    }

    @PostMapping("/rename-file")
    @ResponseBody
    public Map<String, Object> renameFile(@RequestBody Map<String, String> body) {
        Map<String, Object> resp = new HashMap<>();
        String oldName = body.get("oldName");
        String newName = body.get("newName");
        if (oldName == null || newName == null || oldName.isEmpty() || newName.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "Invalid file name");
            return resp;
        }
        Path oldPath = Paths.get(UPLOAD_DIR, oldName);
        Path newPath = Paths.get(UPLOAD_DIR, newName);
        if (!Files.exists(oldPath)) {
            resp.put("success", false);
            resp.put("message", "File not found");
            return resp;
        }
        if (Files.exists(newPath)) {
            resp.put("success", false);
            resp.put("message", "A file with the new name already exists");
            return resp;
        }
        try {
            Files.move(oldPath, newPath);
            resp.put("success", true);
        } catch (IOException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
        }
        return resp;
    }

    // Helper to parse HandBrakeCLI progress (returns percent or -1 if not found)
    private int parseHandbrakeProgress(String line) {
        // HandBrakeCLI progress lines look like: Encoding: task 1 of 1,  12.34 % (x fps, ...)
        int idx = line.indexOf("% ");
        if (idx > 0) {
            int start = line.lastIndexOf(',', idx);
            if (start >= 0) {
                String percentStr = line.substring(start + 1, idx).replaceAll("[^0-9.]", "").trim();
                try {
                    float percent = Float.parseFloat(percentStr);
                    return (int) percent;
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    // Task holder
    private static class ConvertTask {
        private final StringBuilder log = new StringBuilder();
        private final AtomicInteger progress = new AtomicInteger(0);
        private volatile boolean done = false;
        public void appendLog(String s) { log.append(s); }
        public String getLog() { return log.toString(); }
        public int getProgress() { return progress.get(); }
        public void setProgress(int p) { progress.set(p); }
        public boolean isDone() { return done; }
        public void setDone(boolean d) { done = d; }
    }
} 