package com.example.local_cloud.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

        model.addAttribute("videos", videoFiles);
        model.addAttribute("selectedVideo", "");
        model.addAttribute("startValue", "");
        model.addAttribute("endValue", "");
        return "cut";
    }

    @PostMapping
    public String cutVideo(@RequestParam("video") String video,
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "accurateCut", defaultValue = "false") boolean accurateCut,
            Model model) {

        // Check if FFmpeg is installed
        if (!isFFmpegInstalled()) {
            model.addAttribute("error", getFFmpegInstallInstructions());
            
            // Re-populate the form data
            File uploadFolder = new File(UPLOAD_DIR);
            List<String> videoFiles = Arrays.stream(Objects.requireNonNull(uploadFolder.listFiles()))
                    .filter(file -> file.isFile() && file.getName().matches(".*\\.(mp4|mov|avi|mkv)$"))
                    .map(File::getName)
                    .collect(Collectors.toList());
                    
            model.addAttribute("videos", videoFiles);
            model.addAttribute("selectedVideo", video);
            model.addAttribute("startValue", start);
            model.addAttribute("endValue", end);
            model.addAttribute("ffmpegWarning", getFFmpegInstallInstructions());
            return "cut";
        }

        File cutFolder = new File(CUT_DIR);
        if (!cutFolder.exists())
            cutFolder.mkdirs();

        String inputPath = UPLOAD_DIR + File.separator + video;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputName = "cut_" + timestamp + "_" + video;
        String outputPath = CUT_DIR + File.separator + outputName;

        List<String> command;
        
        if (accurateCut) {
            // Method 1: More accurate but slower (re-encodes the video)
            command = Arrays.asList(
                    "ffmpeg", "-i", inputPath,
                    "-ss", start, "-to", end,
                    "-async", "1",
                    "-c:v", "libx264", "-c:a", "aac",
                    "-preset", "fast", "-crf", "22",
                    outputPath);
        } else {
            // Method 2: Faster but less accurate for some videos
            // First seek (fast) to a position before the start time, then cut precisely
            // This helps ensure we capture the desired frames
            
            // Parse start time to see if we need to add safety margin
            float startTime = parseTimeToSeconds(start);
            String seekStart = startTime <= 5 ? "0" : String.format("%.2f", startTime - 1);
            
            command = Arrays.asList(
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
            
            // Capture the output for debugging
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("[FFmpeg] " + line); // Log to console
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

        // Check if output file exists and has size > 0
        File outputFile = new File(outputPath);
        if (!outputFile.exists() || outputFile.length() == 0) {
            model.addAttribute("error", "❌ Output file was not created or is empty. Please check the start and end times.");
            model.addAttribute("ffmpegOutput", output.toString());
        }

        // Reload the list of available videos
        List<String> videoFiles = Arrays.stream(Objects.requireNonNull(new File(UPLOAD_DIR).listFiles()))
                .filter(file -> file.isFile() && file.getName().matches(".*\\.(mp4|mov|avi|mkv)$"))
                .map(File::getName)
                .collect(Collectors.toList());

        model.addAttribute("videos", videoFiles);
        model.addAttribute("selectedVideo", video);
        model.addAttribute("startValue", start);
        model.addAttribute("endValue", end);
        model.addAttribute("accurateCut", accurateCut);
        
        // Only add download link if no error occurred
        if (!model.containsAttribute("error")) {
            model.addAttribute("openFolder", true);
            model.addAttribute("success", "✅ Cut successful!");
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
}
