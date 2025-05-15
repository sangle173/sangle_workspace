package com.example.local_cloud.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cut")
public class VideoCutController {

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/Desktop/LocalCloudUploads";
    private static final String CUT_DIR = System.getProperty("user.home") + "/Desktop/LocalCloudCuts";

    /**
     * Check if FFmpeg is installed and available in the system path
     * @return true if FFmpeg is installed, false otherwise
     */
    private boolean isFFmpegInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                    if (line.contains("ffmpeg version")) {
                        // Found version info, no need to read more
                        break;
                    }
                }
            }
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0 && output.toString().contains("ffmpeg version");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Get installation instructions for FFmpeg on Ubuntu
     * @return A string with the installation instructions
     */
    private String getFFmpegInstallInstructions() {
        return "FFmpeg is not installed. Please install it using the following commands in terminal:\n\n" +
               "sudo apt update\n" +
               "sudo apt install ffmpeg\n\n" +
               "After installation, please try again.";
    }

    @GetMapping
    public String showCutForm(Model model) {
        File uploadFolder = new File(UPLOAD_DIR);
        File cutFolder = new File(CUT_DIR);
        if (!cutFolder.exists())
            cutFolder.mkdirs();

        // Check if FFmpeg is installed and add a warning if not
        if (!isFFmpegInstalled()) {
            model.addAttribute("ffmpegWarning", getFFmpegInstallInstructions());
        }

        List<String> videoFiles = Arrays.stream(Objects.requireNonNull(uploadFolder.listFiles()))
                .filter(file -> file.isFile() && file.getName().matches(".*\\.(mp4|mov|avi|mkv)$"))
                .map(File::getName)
                .collect(Collectors.toList());

        // Set default selected video if there are videos available
        String defaultVideo = videoFiles.isEmpty() ? "" : videoFiles.get(0);
        
        model.addAttribute("videos", videoFiles);
        model.addAttribute("selectedVideo", defaultVideo);
        model.addAttribute("startValue", "00:00:00");
        model.addAttribute("endValue", "00:00:10");
        return "cut";
    }

    @PostMapping
    public String cutVideo(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "accurateCut", defaultValue = "false") boolean accurateCut,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Check if FFmpeg is installed
        if (!isFFmpegInstalled()) {
            model.addAttribute("error", getFFmpegInstallInstructions());
            model.addAttribute("startValue", start);
            model.addAttribute("endValue", end);
            model.addAttribute("ffmpegWarning", getFFmpegInstallInstructions());
            return "cut";
        }

        if (videoFile == null || videoFile.isEmpty()) {
            model.addAttribute("error", "No video file selected.");
            model.addAttribute("startValue", start);
            model.addAttribute("endValue", end);
            return "cut";
        }

        File uploadFolder = new File(UPLOAD_DIR);
        if (!uploadFolder.exists()) uploadFolder.mkdirs();
        String originalFilename = Objects.requireNonNull(videoFile.getOriginalFilename());
        File savedFile = new File(UPLOAD_DIR, originalFilename);
        try {
            videoFile.transferTo(savedFile);
        } catch (IOException e) {
            model.addAttribute("error", "Failed to save uploaded file: " + e.getMessage());
            model.addAttribute("startValue", start);
            model.addAttribute("endValue", end);
            return "cut";
        }
        String inputPath = savedFile.getAbsolutePath();

        File cutFolder = new File(CUT_DIR);
        if (!cutFolder.exists())
            cutFolder.mkdirs();

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String outputName = "cut_" + timestamp + "_" + originalFilename;
        String outputPath = CUT_DIR + File.separator + outputName;

        java.util.List<String> command;
        if (accurateCut) {
            command = java.util.Arrays.asList(
                    "ffmpeg", "-i", inputPath,
                    "-ss", start, "-to", end,
                    "-async", "1",
                    "-c:v", "libx264", "-c:a", "aac",
                    "-preset", "fast", "-crf", "22",
                    outputPath);
        } else {
            float startTime = parseTimeToSeconds(start);
            String seekStart = startTime <= 5 ? "0" : String.format("%.2f", startTime - 1);
            command = java.util.Arrays.asList(
                    "ffmpeg", "-ss", seekStart, "-i", inputPath,
                    "-ss", startTime <= 5 ? start : "1",
                    "-to", end,
                    "-c", "copy",
                    "-avoid_negative_ts", "make_zero",
                    outputPath);
        }

        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("[FFmpeg] " + line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                model.addAttribute("error", "❌ Failed to cut video (exit code " + exitCode + ")");
                model.addAttribute("ffmpegOutput", output.toString());
            }
        } catch (Exception e) {
            model.addAttribute("error", "❌ Failed to cut video: " + e.getMessage());
            model.addAttribute("ffmpegOutput", output.toString());
        }

        File outputFile = new File(outputPath);
        if (!outputFile.exists() || outputFile.length() == 0) {
            model.addAttribute("error", "❌ Output file was not created or is empty. Please check the start and end times.");
            model.addAttribute("ffmpegOutput", output.toString());
        }

        if (!model.containsAttribute("error")) {
            redirectAttributes.addFlashAttribute("openFolder", true);
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Cut successful!");
            redirectAttributes.addFlashAttribute("download", "/cut/download/" + outputName);
            redirectAttributes.addFlashAttribute("outputFile", outputName);
            redirectAttributes.addFlashAttribute("originalVideo", originalFilename);
            redirectAttributes.addFlashAttribute("startValue", start);
            redirectAttributes.addFlashAttribute("endValue", end);
            redirectAttributes.addFlashAttribute("accurateCut", accurateCut);
            return "redirect:/cut";
        }
        return "cut";
    }
    
    /**
     * Parse time format (HH:MM:SS or MM:SS or SS or SS.ms) to seconds
     */
    private float parseTimeToSeconds(String timeStr) {
        try {
            // Split by colons
            String[] parts = timeStr.split(":");
            float seconds = 0;
            
            if (parts.length == 3) {
                // HH:MM:SS format
                seconds += Integer.parseInt(parts[0]) * 3600; // Hours
                seconds += Integer.parseInt(parts[1]) * 60;   // Minutes
                seconds += Float.parseFloat(parts[2]);       // Seconds (with possible decimals)
            } else if (parts.length == 2) {
                // MM:SS format
                seconds += Integer.parseInt(parts[0]) * 60;   // Minutes
                seconds += Float.parseFloat(parts[1]);       // Seconds (with possible decimals)
            } else {
                // Just seconds (possibly with decimal)
                seconds = Float.parseFloat(timeStr);
            }
            
            return seconds;
        } catch (NumberFormatException e) {
            // If parsing fails, return 0
            return 0;
        }
    }
    
    @GetMapping("/verify-duration")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyDuration(
            @RequestParam("video") String video,
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!isFFmpegInstalled()) {
            response.put("success", false);
            response.put("message", "FFmpeg is not installed");
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(response);
        }
        
        try {
            String inputPath = UPLOAD_DIR + File.separator + video;
            
            // Get video duration
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", 
                "default=noprint_wrappers=1:nokey=1", inputPath
            );
            
            Process process = pb.start();
            String durationStr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                durationStr = reader.readLine();
            }
            
            process.waitFor();
            
            if (durationStr == null) {
                response.put("success", false);
                response.put("message", "Could not get video duration");
                return ResponseEntity.ok(response);
            }
            
            float duration = Float.parseFloat(durationStr);
            float startSec = parseTimeToSeconds(start);
            float endSec = parseTimeToSeconds(end);
            
            if (startSec >= endSec) {
                response.put("success", false);
                response.put("message", "Start time must be less than end time");
                return ResponseEntity.ok(response);
            }
            
            if (endSec > duration) {
                response.put("success", false);
                response.put("message", "End time exceeds video duration (" + formatSeconds(duration) + ")");
                return ResponseEntity.ok(response);
            }
            
            response.put("success", true);
            response.put("duration", duration);
            response.put("formattedDuration", formatSeconds(duration));
            response.put("cutDuration", endSec - startSec);
            response.put("formattedCutDuration", formatSeconds(endSec - startSec));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Open the cuts folder in the system file explorer
     */
    @PostMapping("/open-folder")
    @ResponseBody
    public ResponseEntity<?> openCutsFolder() {
        try {
            // First attempt to use nautilus (common on Ubuntu/GNOME)
            ProcessBuilder pb = new ProcessBuilder("nautilus", "--new-window", CUT_DIR);
            pb.environment().put("DISPLAY", ":0");
            pb.start();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            try {
                // Fallback to xdg-open (general Linux method)
                ProcessBuilder pb2 = new ProcessBuilder("xdg-open", CUT_DIR);
                pb2.environment().put("DISPLAY", ":0");
                pb2.start();
                return ResponseEntity.ok().build();
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                       .body("Failed to open cuts folder: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Format seconds to HH:MM:SS.ms format
     */
    private String formatSeconds(float seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        float secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%06.3f", hours, minutes, secs);
        } else {
            return String.format("%02d:%06.3f", minutes, secs);
        }
    }

    /**
     * Serve a cut video file for download or viewing
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> serveVideo(@PathVariable String filename) {
        try {
            File file = new File(CUT_DIR + File.separator + filename);
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .header("Content-Type", "video/mp4")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
