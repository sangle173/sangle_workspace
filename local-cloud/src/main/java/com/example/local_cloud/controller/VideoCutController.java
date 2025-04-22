package com.example.local_cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cut")
public class VideoCutController {

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/Desktop/LocalCloudUploads";
    private static final String CUT_DIR = System.getProperty("user.home") + "/Desktop/LocalCloudCuts";

    @GetMapping
    public String showCutForm(Model model) {
        File uploadFolder = new File(UPLOAD_DIR);
        File cutFolder = new File(CUT_DIR);
        if (!cutFolder.exists())
            cutFolder.mkdirs();

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
            Model model) {

        File cutFolder = new File(CUT_DIR);
        if (!cutFolder.exists())
            cutFolder.mkdirs();

        String inputPath = UPLOAD_DIR + File.separator + video;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputName = "cut_" + timestamp + "_" + video;
        String outputPath = CUT_DIR + File.separator + outputName;

        List<String> command = Arrays.asList(
                "ffmpeg", "-i", inputPath,
                "-ss", start, "-to", end,
                "-c", "copy", outputPath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            model.addAttribute("error", "❌ Failed to cut video: " + e.getMessage());
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
        model.addAttribute("download", "/cuts/" + outputName);
        model.addAttribute("success", "✅ Cut successful!");
        return "cut";
    }
}
