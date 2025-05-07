package com.example.local_cloud.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/server-files")
public class LocalServerController {
    private static final String DESKTOP = System.getProperty("user.home") + File.separator + "Desktop";
    private static final String ROOT_DIR = DESKTOP + File.separator + "LocalServerFolder";

    static {
        try {
            Files.createDirectories(Paths.get(ROOT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping({"", "/", "/*"})
    public String listFiles(@RequestParam(value = "path", required = false, defaultValue = "") String relPath, Model model) {
        Path dir = Paths.get(ROOT_DIR, relPath);
        File folder = dir.toFile();
        if (!folder.exists() || !folder.isDirectory()) {
            model.addAttribute("error", "Folder not found");
            return "server_files";
        }
        File[] files = folder.listFiles();
        List<Map<String, Object>> fileList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::isFile).thenComparing(File::getName));
            for (File f : files) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", f.getName());
                entry.put("isDir", f.isDirectory());
                entry.put("size", f.isFile() ? f.length() : null);
                entry.put("sizeDisplay", f.isFile() ? humanReadableByteCountBin(f.length()) : "");
                entry.put("created", sdf.format(new Date(f.lastModified())));
                entry.put("link", f.isFile() ? "/server-files/download?path=" + URLEncoder.encode(relPath + (relPath.isEmpty() ? "" : "/") + f.getName(), StandardCharsets.UTF_8) : null);
                entry.put("browse", f.isDirectory() ? "/server-files?path=" + URLEncoder.encode(relPath + (relPath.isEmpty() ? "" : "/") + f.getName(), StandardCharsets.UTF_8) : null);
                fileList.add(entry);
            }
        }
        // For navigation
        String parent = null;
        if (!relPath.isEmpty()) {
            int idx = relPath.lastIndexOf('/');
            parent = idx > 0 ? relPath.substring(0, idx) : "";
        }
        model.addAttribute("files", fileList);
        model.addAttribute("relPath", relPath);
        model.addAttribute("parent", parent);
        return "server_files";
    }

    private static String humanReadableByteCountBin(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp-1) + (exp > 1 ? "" : "");
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @GetMapping("/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String relPath) throws IOException {
        Path file = Paths.get(ROOT_DIR, relPath);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file.toFile());
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName().toString() + "\"")
                .body(resource);
    }

    @PostMapping("/open-localserverfolder")
    @ResponseBody
    public ResponseEntity<?> openLocalServerFolder() {
        try {
            ProcessBuilder pb = new ProcessBuilder("nautilus", "--new-window", ROOT_DIR);
            pb.environment().put("DISPLAY", ":0");
            pb.start();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            try {
                ProcessBuilder pb2 = new ProcessBuilder("xdg-open", ROOT_DIR);
                pb2.environment().put("DISPLAY", ":0");
                pb2.start();
                return ResponseEntity.ok().build();
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to open folder: " + ex.getMessage());
            }
        }
    }

    @GetMapping("/ios-devices")
    @ResponseBody
    public List<Map<String, String>> getIOSDevices() throws IOException {
        List<Map<String, String>> devices = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("idevice_id", "-l");
        Process process = pb.start();
        List<String> udids = new ArrayList<>();
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) udids.add(line);
            }
        }
        for (String udid : udids) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("UniqueDeviceID", udid);
            ProcessBuilder pbInfo = new ProcessBuilder("ideviceinfo", "-u", udid);
            Process procInfo = pbInfo.start();
            try (Scanner scanner = new Scanner(procInfo.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        info.put(key, value);
                    }
                }
            }
            devices.add(info);
        }
        return devices;
    }

    @PostMapping(value = "/install-ipa", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String installIPA(@RequestBody Map<String, String> body) throws IOException, InterruptedException {
        String relPath = body.get("path");
        if (relPath == null || !relPath.endsWith(".ipa")) return "Invalid IPA file.";
        Path ipaFile = Paths.get(ROOT_DIR, relPath);
        if (!Files.exists(ipaFile)) return "IPA file not found.";
        ProcessBuilder pb = new ProcessBuilder("ideviceinstaller", "-i", ipaFile.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder log = new StringBuilder();
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                log.append(scanner.nextLine()).append("\n");
            }
        }
        process.waitFor();
        return log.toString();
    }

    @GetMapping("/android-devices")
    @ResponseBody
    public List<Map<String, String>> getAndroidDevices() throws IOException {
        List<Map<String, String>> devices = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("adb", "devices");
        Process process = pb.start();
        List<String> deviceIds = new ArrayList<>();
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            boolean headerSkipped = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!headerSkipped) { // skip the first line
                    headerSkipped = true;
                    continue;
                }
                if (line.isEmpty() || line.endsWith("offline") || line.endsWith("unauthorized")) continue;
                if (line.endsWith("device")) {
                    String id = line.split("\t")[0];
                    deviceIds.add(id);
                }
            }
        }
        for (String id : deviceIds) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("DeviceId", id);
            // Get device properties
            ProcessBuilder pbInfo = new ProcessBuilder("adb", "-s", id, "shell", "getprop");
            Process procInfo = pbInfo.start();
            try (Scanner scanner = new Scanner(procInfo.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains(":")) {
                        if (line.contains("[ro.product.model]")) {
                            String[] parts = line.split("\\]: \\[", 2);
                            if (parts.length > 1) info.put("Model", parts[1].replace("]", ""));
                        } else if (line.contains("[ro.product.manufacturer]")) {
                            String[] parts = line.split("\\]: \\[", 2);
                            if (parts.length > 1) info.put("Manufacturer", parts[1].replace("]", ""));
                        } else if (line.contains("[ro.build.version.release]")) {
                            String[] parts = line.split("\\]: \\[", 2);
                            if (parts.length > 1) info.put("AndroidVersion", parts[1].replace("]", ""));
                        } else if (line.contains("[ro.product.brand]")) {
                            String[] parts = line.split("\\]: \\[", 2);
                            if (parts.length > 1) info.put("Brand", parts[1].replace("]", ""));
                        }
                    }
                }
            }
            devices.add(info);
        }
        return devices;
    }

    @PostMapping(value = "/install-apk", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String installAPK(@RequestBody Map<String, String> body) throws IOException, InterruptedException {
        String relPath = body.get("path");
        String deviceId = body.get("deviceId");
        if (relPath == null || !relPath.endsWith(".apk")) return "Invalid APK file.";
        if (deviceId == null || deviceId.isEmpty()) return "No device selected.";
        Path apkFile = Paths.get(ROOT_DIR, relPath);
        if (!Files.exists(apkFile)) return "APK file not found.";
        ProcessBuilder pb = new ProcessBuilder("adb", "-s", deviceId, "install", "-r", apkFile.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder log = new StringBuilder();
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                log.append(scanner.nextLine()).append("\n");
            }
        }
        process.waitFor();
        return log.toString();
    }
} 