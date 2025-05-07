package com.example.local_cloud.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.MediaType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.PostMapping;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.URL;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class DownloadController {

    private static final String DESKTOP = System.getProperty("user.home") + File.separator + "Desktop";
    private static final String FILE_DIR = DESKTOP + File.separator + "LocalDownloadFolder" + File.separator;
    private static final Map<String, String> downloadStatus = new ConcurrentHashMap<>();
    private static final Map<String, Integer> downloadProgress = new ConcurrentHashMap<>();
    private static final ExecutorService downloadExecutor = Executors.newCachedThreadPool();

    static {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(FILE_DIR));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/files")
    public String listFiles(Model model) {
        File folder = new File(FILE_DIR);
        File[] files = folder.listFiles();
        List<String> filenames = files == null ? List.of() : Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());
        model.addAttribute("files", filenames);
        return "files";
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        Path file = Paths.get(FILE_DIR).resolve(filename);
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(resource);
    }

    @GetMapping(value = "/fetch-links", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<String> fetchLinks(@RequestParam("url") String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        return links.stream()
                .map(link -> link.attr("href").matches("^https?://.*") ? link.attr("href") : link.absUrl("href"))
                .filter(href -> href.matches(".*\\.([a-zA-Z0-9]{2,5})(\\?.*)?$"))
                .distinct()
                .collect(Collectors.toList());
    }

    @PostMapping(value = "/server-download", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> serverDownload(@RequestBody List<String> urls) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> result = new HashMap<>();
        String folderName = "Download_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path targetDir = Paths.get(FILE_DIR, folderName);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            response.put("error", "Failed to create target directory: " + e.getMessage());
            response.put("result", result);
            return response;
        }
        downloadStatus.clear();
        downloadProgress.clear();
        for (String fileUrl : urls) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1).split("[?&#]")[0];
            downloadStatus.put(fileName, "Waiting");
            downloadProgress.put(fileName, 0);
            downloadExecutor.submit(() -> {
                try {
                    downloadStatus.put(fileName, "Downloading");
                    URL url = new URL(fileUrl);
                    Path dest = targetDir.resolve(fileName);
                    long totalBytes = 0;
                    try {
                        totalBytes = url.openConnection().getContentLengthLong();
                    } catch (Exception ignored) {}
                    try (var in = url.openStream(); var out = Files.newOutputStream(dest)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        long downloaded = 0;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                            downloaded += len;
                            if (totalBytes > 0) {
                                int percent = (int) (downloaded * 100 / totalBytes);
                                downloadProgress.put(fileName, percent);
                            }
                        }
                        if (totalBytes > 0) downloadProgress.put(fileName, 100);
                    }
                    downloadStatus.put(fileName, "Success");
                } catch (Exception e) {
                    downloadStatus.put(fileName, "Failed");
                    downloadProgress.put(fileName, 0);
                }
            });
        }
        response.put("folder", folderName);
        response.put("result", downloadStatus);
        return response;
    }

    @GetMapping(value = "/server-download-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getServerDownloadStatus() {
        Map<String, Object> result = new HashMap<>();
        for (String file : downloadStatus.keySet()) {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("status", downloadStatus.get(file));
            fileInfo.put("progress", downloadProgress.getOrDefault(file, 0));
            result.put(file, fileInfo);
        }
        return result;
    }
} 