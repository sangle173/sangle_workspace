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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/server-files")
public class LocalServerController {
    private static final String DESKTOP = System.getProperty("user.home") + File.separator + "Desktop";
    private static final String ROOT_DIR = DESKTOP + File.separator + "LocalServerFolder";
    private static final String BUILDS_DIR = ROOT_DIR + File.separator + "Builds";
    private static final String IOS_BUILDS_DIR = BUILDS_DIR + File.separator + "iOS";
    private static final String ANDROID_BUILDS_DIR = BUILDS_DIR + File.separator + "Android";
    
    // Base URL for build downloads
    private static final String IOS_BUILD_SERVER_BASE_URL = "http://172.18.100.184:5000/logigear/passport-ios-generic-local-main/";
    private static final String ANDROID_BUILD_SERVER_BASE_URL = "http://172.18.100.184:5000/logigear/passport-android-generic-local-main/";

    static {
        try {
            Files.createDirectories(Paths.get(ROOT_DIR));
            Files.createDirectories(Paths.get(BUILDS_DIR));
            Files.createDirectories(Paths.get(IOS_BUILDS_DIR));
            Files.createDirectories(Paths.get(ANDROID_BUILDS_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping({"", "/", "/*"})
    public String listFiles(@RequestParam(value = "path", required = false, defaultValue = "") String relPath, Model model) {
        Path dir = Paths.get(ROOT_DIR, relPath);
        File folder = dir.toFile();
        if (!folder.exists() || !folder.isDirectory()) {
            model.addAttribute("error", "Folder not found: " + relPath);
            model.addAttribute("files", new ArrayList<>()); // Initialize with empty list
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
                
                // Use original URL format 
                entry.put("link", f.isFile() ? "/server-files/download?path=" + URLEncoder.encode(relPath + (relPath.isEmpty() ? "" : "/") + f.getName(), StandardCharsets.UTF_8) : null);
                entry.put("browse", f.isDirectory() ? "/server-files?path=" + URLEncoder.encode(relPath + (relPath.isEmpty() ? "" : "/") + f.getName(), StandardCharsets.UTF_8) : null);
                
                fileList.add(entry);
            }
        } else {
            model.addAttribute("error", "Error listing files in directory: " + relPath);
        }
        
        // Make sure the Builds directory is visible in the root folder
        if (relPath.isEmpty()) {
            File buildsDir = new File(BUILDS_DIR);
            if (buildsDir.exists() && buildsDir.isDirectory()) {
                boolean buildsFound = false;
                for (Map<String, Object> entry : fileList) {
                    if ("Builds".equals(entry.get("name"))) {
                        buildsFound = true;
                        break;
                    }
                }
                
                if (!buildsFound) {
                    Map<String, Object> buildsEntry = new HashMap<>();
                    buildsEntry.put("name", "Builds");
                    buildsEntry.put("isDir", true);
                    buildsEntry.put("created", sdf.format(new Date(buildsDir.lastModified())));
                    buildsEntry.put("browse", "/server-files?path=Builds");
                    fileList.add(buildsEntry);
                    
                    // Resort the list
                    fileList.sort((a, b) -> {
                        boolean aIsDir = (boolean) a.get("isDir");
                        boolean bIsDir = (boolean) b.get("isDir");
                        if (aIsDir != bIsDir) {
                            return aIsDir ? -1 : 1;
                        }
                        return ((String) a.get("name")).compareTo((String) b.get("name"));
                    });
                }
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

    @PostMapping("/open-subfolder")
    @ResponseBody
    public ResponseEntity<?> openSubFolder(@RequestBody Map<String, String> body) {
        String relPath = body.get("path");
        if (relPath == null) {
            return ResponseEntity.badRequest().body("No path specified");
        }
        
        Path folderPath = Paths.get(ROOT_DIR, relPath);
        File folder = folderPath.toFile();
        
        if (!folder.exists() || !folder.isDirectory()) {
            return ResponseEntity.badRequest().body("Folder not found or not a directory");
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder("nautilus", "--new-window", folderPath.toString());
            pb.environment().put("DISPLAY", ":0");
            pb.start();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            try {
                ProcessBuilder pb2 = new ProcessBuilder("xdg-open", folderPath.toString());
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
    public List<Map<String, String>> getIOSDevices() throws IOException, InterruptedException {
        List<Map<String, String>> devices = new ArrayList<>();
        
        // Check if libimobiledevice is installed
        if (!isIdeviceIdInstalled()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "idevice_id not installed");
            error.put("message", "To install libimobiledevice tools on Ubuntu, run:\n\nsudo apt update\nsudo apt install libimobiledevice-utils");
            devices.add(error);
            return devices;
        }
        
        ProcessBuilder pb = new ProcessBuilder("idevice_id", "-l");
        Process process = pb.start();
        List<String> udids = new ArrayList<>();
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) udids.add(line);
            }
        }
        
        if (udids.isEmpty()) {
            Map<String, String> info = new HashMap<>();
            info.put("message", "No iOS devices detected. Make sure your device is connected and trusted.");
            devices.add(info);
            return devices;
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
    
    /**
     * Check if idevice_id is installed and accessible
     */
    private boolean isIdeviceIdInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("idevice_id", "--help");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0 || exitCode == 1; // Help command typically returns 1
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @GetMapping("/android-devices")
    @ResponseBody
    public List<Map<String, String>> getAndroidDevices() throws IOException, InterruptedException {
        List<Map<String, String>> devices = new ArrayList<>();
        
        // Check if adb is installed
        if (!isAdbInstalled()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "adb not installed");
            error.put("message", "To install ADB on Ubuntu, run:\n\nsudo apt update\nsudo apt install adb");
            devices.add(error);
            return devices;
        }
        
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
        
        if (deviceIds.isEmpty()) {
            Map<String, String> info = new HashMap<>();
            info.put("message", "No Android devices detected. Make sure your device is connected with USB debugging enabled.");
            devices.add(info);
            return devices;
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
    
    /**
     * Check if ADB is installed and accessible
     */
    private boolean isAdbInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0 && output.toString().contains("Android Debug Bridge");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @PostMapping(value = "/install-ipa", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String installIPA(@RequestBody Map<String, String> body) throws IOException, InterruptedException {
        String relPath = body.get("path");
        if (relPath == null || !relPath.endsWith(".ipa")) return "Invalid IPA file.";
        
        // Check if ideviceinstaller is installed first
        if (!isIdeviceinstallerInstalled()) {
            return "ideviceinstaller is not installed or not accessible.\n\n" +
                   "To install ideviceinstaller on Ubuntu, run these commands in terminal:\n\n" +
                   "sudo apt update\n" +
                   "sudo apt install ideviceinstaller libimobiledevice-utils\n\n" +
                   "After installation, please try again.";
        }
        
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
    
    /**
     * Check if ideviceinstaller is installed and accessible
     */
    private boolean isIdeviceinstallerInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ideviceinstaller", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @PostMapping(value = "/install-apk", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String installAPK(@RequestBody Map<String, String> body) throws IOException, InterruptedException {
        String relPath = body.get("path");
        String deviceId = body.get("deviceId");
        if (relPath == null || !relPath.endsWith(".apk")) return "Invalid APK file.";
        if (deviceId == null || deviceId.isEmpty()) return "No device selected.";
        
        // Check if ADB is installed first
        if (!isAdbInstalled()) {
            return "ADB (Android Debug Bridge) is not installed or not accessible.\n\n" +
                   "To install ADB on Ubuntu, run these commands in terminal:\n\n" +
                   "sudo apt update\n" +
                   "sudo apt install adb\n\n" +
                   "After installation, please try again.";
        }
        
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

    /**
     * Sync the latest iOS build from the build server
     */
    @PostMapping("/sync-latest-ios-build")
    @ResponseBody
    public Map<String, Object> syncLatestIOSBuild() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Starting sync of latest iOS build from " + IOS_BUILD_SERVER_BASE_URL);
            
            // Step 1: Find the latest build number
            int latestBuildNumber = findLatestBuildNumber(IOS_BUILD_SERVER_BASE_URL);
            System.out.println("Latest build number found: " + latestBuildNumber);
            
            if (latestBuildNumber <= 0) {
                String errorMsg = "Could not determine the latest build number. Please verify the build server is accessible.";
                System.err.println(errorMsg);
                response.put("success", false);
                response.put("message", errorMsg);
                return response;
            }
            
            // Check if we already have the latest build
            File iosBuildsDir = new File(IOS_BUILDS_DIR);
            File[] existingFiles = iosBuildsDir.listFiles((dir, name) -> name.startsWith(latestBuildNumber + "_"));
            
            if (existingFiles != null && existingFiles.length > 0) {
                String fileName = existingFiles[0].getName();
                String alreadySyncedMsg = "You already have the latest iOS build #" + latestBuildNumber + " (" + fileName + ")";
                System.out.println(alreadySyncedMsg);
                response.put("success", true);
                response.put("alreadySynced", true);
                response.put("message", alreadySyncedMsg);
                response.put("buildNumber", latestBuildNumber);
                response.put("fileName", fileName);
                response.put("filePath", "Builds/iOS/" + fileName);
                return response;
            }
            
            // Step 2: Download the IPA file from the build directory
            String buildUrl = IOS_BUILD_SERVER_BASE_URL + latestBuildNumber + "/build/dev/";
            System.out.println("Looking for IPA file in: " + buildUrl);
            
            Map<String, String> ipaInfo = findIPAFileUrl(buildUrl);
            
            if (ipaInfo == null || ipaInfo.get("url") == null) {
                String errorMsg = "No IPA file found in build #" + latestBuildNumber;
                System.err.println(errorMsg);
                response.put("success", false);
                response.put("message", errorMsg);
                return response;
            }
            
            String ipaUrl = ipaInfo.get("url");
            String originalFileName = ipaInfo.get("fileName");
            
            System.out.println("IPA URL found: " + ipaUrl);
            System.out.println("Original file name: " + originalFileName);
            
            // Step 3: Download the IPA file - prefix filename with build number
            String prefixedFileName = latestBuildNumber + "_" + originalFileName;
            Path targetPath = Paths.get(IOS_BUILDS_DIR, prefixedFileName);
            System.out.println("Downloading to: " + targetPath);
            
            boolean downloadSuccess = downloadFile(ipaUrl, targetPath);
            
            if (downloadSuccess) {
                String successMsg = "Successfully synced iOS build #" + latestBuildNumber + " (" + prefixedFileName + ")";
                System.out.println(successMsg);
                response.put("success", true);
                response.put("message", successMsg);
                response.put("buildNumber", latestBuildNumber);
                response.put("fileName", prefixedFileName);
                response.put("filePath", "Builds/iOS/" + prefixedFileName);
            } else {
                String errorMsg = "Failed to download IPA file from build #" + latestBuildNumber;
                System.err.println(errorMsg);
                response.put("success", false);
                response.put("message", errorMsg);
            }
            
            return response;
        } catch (Exception e) {
            String errorMsg = "Error during sync: " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();
            response.put("success", false);
            response.put("message", errorMsg);
            response.put("exceptionType", e.getClass().getName());
            return response;
        }
    }
    
    /**
     * Sync the latest Android build from the build server
     */
    @PostMapping("/sync-latest-android-build")
    @ResponseBody
    public Map<String, Object> syncLatestAndroidBuild() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Starting sync of latest Android build from " + ANDROID_BUILD_SERVER_BASE_URL);
            
            // Step 1: Find the latest build number
            int latestBuildNumber = findLatestBuildNumber(ANDROID_BUILD_SERVER_BASE_URL);
            System.out.println("Latest build number found: " + latestBuildNumber);
            
            if (latestBuildNumber <= 0) {
                String errorMsg = "Could not determine the latest build number. Please verify the build server is accessible.";
                System.err.println(errorMsg);
                response.put("success", false);
                response.put("message", errorMsg);
                return response;
            }
            
            // Check if we already have the latest build
            File androidBuildsDir = new File(ANDROID_BUILDS_DIR);
            File[] existingFiles = androidBuildsDir.listFiles((dir, name) -> name.startsWith(latestBuildNumber + "_"));
            
            if (existingFiles != null && existingFiles.length > 0) {
                String fileName = existingFiles[0].getName();
                String alreadySyncedMsg = "You already have the latest Android build #" + latestBuildNumber + " (" + fileName + ")";
                System.out.println(alreadySyncedMsg);
                response.put("success", true);
                response.put("alreadySynced", true);
                response.put("message", alreadySyncedMsg);
                response.put("buildNumber", latestBuildNumber);
                response.put("fileName", fileName);
                response.put("filePath", "Builds/Android/" + fileName);
                return response;
            }
            
            // Step 2: Download the APK file from the build directory
            String buildUrl = ANDROID_BUILD_SERVER_BASE_URL + latestBuildNumber + "/build/";
            System.out.println("Looking for APK file in: " + buildUrl);
            
            Map<String, String> apkInfo = findAPKFileUrl(buildUrl);
            
            if (apkInfo == null || apkInfo.get("url") == null) {
                String errorMsg = "No APK file found in build #" + latestBuildNumber;
                System.err.println(errorMsg);
                response.put("success", false);
                response.put("message", errorMsg);
                return response;
            }
            
            String apkUrl = apkInfo.get("url");
            String originalFileName = apkInfo.get("fileName");
            
            System.out.println("APK URL found: " + apkUrl);
            System.out.println("Original file name: " + originalFileName);
            
            // Step 3: Download the APK file with build number prefix
            String prefixedFileName = latestBuildNumber + "_" + originalFileName;
            Path targetPath = Paths.get(ANDROID_BUILDS_DIR, prefixedFileName);
            System.out.println("Downloading to: " + targetPath);
            
            boolean downloadSuccess = downloadFile(apkUrl, targetPath);
            
            if (downloadSuccess) {
                String successMsg = "Successfully synced Android build #" + latestBuildNumber + " (" + prefixedFileName + ")";
                System.out.println(successMsg);
                response.put("success", true);
                response.put("message", successMsg);
                response.put("buildNumber", latestBuildNumber);
                response.put("fileName", prefixedFileName);
                response.put("filePath", "Builds/Android/" + prefixedFileName);
            } else {
                String errorMsg = "Failed to download APK file from build #" + latestBuildNumber;
                System.err.println(errorMsg);
                response.put("success", false);
                response.put("message", errorMsg);
            }
            
            return response;
        } catch (Exception e) {
            String errorMsg = "Error during sync: " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();
            response.put("success", false);
            response.put("message", errorMsg);
            response.put("exceptionType", e.getClass().getName());
            return response;
        }
    }
    
    /**
     * Sync both iOS and Android latest builds
     */
    @PostMapping("/sync-latest-builds")
    @ResponseBody
    public Map<String, Object> syncLatestBuilds() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> iosResult = new HashMap<>();
        Map<String, Object> androidResult = new HashMap<>();
        
        // First sync iOS build
        try {
            iosResult = syncLatestIOSBuild();
        } catch (Exception e) {
            iosResult.put("success", false);
            iosResult.put("message", "Error syncing iOS build: " + e.getMessage());
        }
        
        // Then sync Android build
        try {
            androidResult = syncLatestAndroidBuild();
        } catch (Exception e) {
            androidResult.put("success", false);
            androidResult.put("message", "Error syncing Android build: " + e.getMessage());
        }
        
        // Check if at least one build is new (not already synced)
        boolean allAlreadySynced = 
            (boolean)iosResult.getOrDefault("alreadySynced", false) && 
            (boolean)androidResult.getOrDefault("alreadySynced", false);
        
        // Set overall success status - still a success if builds were already synced
        boolean overallSuccess = (boolean)iosResult.getOrDefault("success", false) || 
                                (boolean)androidResult.getOrDefault("success", false);
        
        response.put("success", overallSuccess);
        response.put("allAlreadySynced", allAlreadySynced);
        response.put("ios", iosResult);
        response.put("android", androidResult);
        
        return response;
    }
    
    /**
     * Find the latest build number from the build server
     */
    private int findLatestBuildNumber(String serverBaseUrl) throws IOException {
        System.out.println("Connecting to build server: " + serverBaseUrl);
        URL url = new URL(serverBaseUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(30000);    // 30 seconds
        
        int responseCode = connection.getResponseCode();
        System.out.println("Build server response code: " + responseCode);
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("Failed to connect to build server. Response code: " + responseCode);
            return 0;
        }
        
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            
            String htmlContent = content.toString();
            System.out.println("Received HTML content length: " + htmlContent.length() + " bytes");
            
            // Save the first 500 characters for debugging
            String contentPreview = htmlContent.length() > 500 ? 
                htmlContent.substring(0, 500) + "..." : htmlContent;
            System.out.println("Content preview: " + contentPreview);
            
            // Exact pattern matching the example HTML structure
            Pattern pattern = Pattern.compile("<tr><td valign=\"top\"><img src=\"/icons/folder\\.gif\" alt=\"\\[DIR\\]\"></td><td><a href=\"(\\d+)/\">(\\d+)/</a></td>");
            Matcher matcher = pattern.matcher(htmlContent);
            
            int highestBuildNumber = 0;
            int matchCount = 0;
            
            while (matcher.find()) {
                matchCount++;
                try {
                    String folderName = matcher.group(1); // The folder number captured in first group
                    int buildNumber = Integer.parseInt(folderName);
                    System.out.println("Found build number: " + buildNumber);
                    if (buildNumber > highestBuildNumber) {
                        highestBuildNumber = buildNumber;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Found non-numeric directory: " + matcher.group(1));
                    // Skip non-numeric directories
                }
            }
            
            System.out.println("Total build directories found: " + matchCount);
            System.out.println("Highest build number found: " + highestBuildNumber);
            
            if (matchCount == 0) {
                // Try a more lenient pattern as backup
                System.out.println("No matches found with exact pattern, trying more lenient pattern...");
                Pattern lenientPattern = Pattern.compile("folder.gif[^<]*</td><td><a href=\"(\\d+)/\">");
                Matcher lenientMatcher = lenientPattern.matcher(htmlContent);
                
                while (lenientMatcher.find()) {
                    matchCount++;
                    try {
                        String folderName = lenientMatcher.group(1);
                        int buildNumber = Integer.parseInt(folderName);
                        System.out.println("Found build number (lenient pattern): " + buildNumber);
                        if (buildNumber > highestBuildNumber) {
                            highestBuildNumber = buildNumber;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Found non-numeric directory: " + lenientMatcher.group(1));
                    }
                }
                
                System.out.println("Total build directories found (lenient pattern): " + matchCount);
                System.out.println("Highest build number found: " + highestBuildNumber);
                
                // Last resort - try an ultra-lenient pattern
                if (matchCount == 0) {
                    System.out.println("Still no matches, trying ultra-lenient pattern...");
                    Pattern ultraPattern = Pattern.compile("href=\"(\\d+)/\">");
                    Matcher ultraMatcher = ultraPattern.matcher(htmlContent);
                    
                    while (ultraMatcher.find()) {
                        matchCount++;
                        try {
                            String folderName = ultraMatcher.group(1);
                            int buildNumber = Integer.parseInt(folderName);
                            System.out.println("Found build number (ultra pattern): " + buildNumber);
                            if (buildNumber > highestBuildNumber) {
                                highestBuildNumber = buildNumber;
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Found non-numeric directory: " + ultraMatcher.group(1));
                        }
                    }
                    
                    System.out.println("Total build directories found (ultra pattern): " + matchCount);
                    System.out.println("Highest build number found: " + highestBuildNumber);
                }
            }
            
            return highestBuildNumber;
        } catch (Exception e) {
            System.err.println("Error parsing build server response: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Find the IPA file URL in the given build directory
     * @return Map containing the URL and original filename
     */
    private Map<String, String> findIPAFileUrl(String buildDirectoryUrl) throws IOException {
        System.out.println("Looking for IPA files in: " + buildDirectoryUrl);
        URL url = new URL(buildDirectoryUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(30000);    // 30 seconds
        
        int responseCode = connection.getResponseCode();
        System.out.println("Build directory response code: " + responseCode);
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("Failed to access build directory. Response code: " + responseCode);
            return null;
        }
        
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            
            String htmlContent = content.toString();
            System.out.println("Received HTML content length: " + htmlContent.length() + " bytes");
            
            // Parse HTML content to find IPA files - look for links ending with .ipa
            Pattern pattern = Pattern.compile("href=\"([^\"]+\\.ipa)\"");
            Matcher matcher = pattern.matcher(htmlContent);
            
            if (matcher.find()) {
                String ipaFileName = matcher.group(1);
                System.out.println("Found IPA file: " + ipaFileName);
                
                Map<String, String> result = new HashMap<>();
                result.put("url", buildDirectoryUrl + ipaFileName);
                result.put("fileName", ipaFileName);
                return result;
            }
            
            System.err.println("No IPA file found in directory");
            return null;
        } catch (Exception e) {
            System.err.println("Error searching for IPA files: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Find the APK file URL in the given build directory
     * @return Map containing the URL and original filename
     */
    private Map<String, String> findAPKFileUrl(String buildDirectoryUrl) throws IOException {
        System.out.println("Looking for APK files in: " + buildDirectoryUrl);
        URL url = new URL(buildDirectoryUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(30000);    // 30 seconds
        
        int responseCode = connection.getResponseCode();
        System.out.println("Build directory response code: " + responseCode);
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("Failed to access build directory. Response code: " + responseCode);
            return null;
        }
        
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            
            String htmlContent = content.toString();
            System.out.println("Received HTML content length: " + htmlContent.length() + " bytes");
            
            // Parse HTML content to find APK files - look for links containing "*arm64-v8a-allowClearTextTraffic-release.apk"
            Pattern pattern = Pattern.compile("href=\"([^\"]+arm64-v8a-allowClearTextTraffic-release\\.apk)\"");
            Matcher matcher = pattern.matcher(htmlContent);
            
            if (matcher.find()) {
                String apkFileName = matcher.group(1);
                System.out.println("Found APK file: " + apkFileName);
                
                Map<String, String> result = new HashMap<>();
                result.put("url", buildDirectoryUrl + apkFileName);
                result.put("fileName", apkFileName);
                return result;
            }
            
            // If specific pattern not found, try a more general one
            System.out.println("Specific APK pattern not found, trying general pattern...");
            Pattern generalPattern = Pattern.compile("href=\"([^\"]+\\.apk)\"");
            Matcher generalMatcher = generalPattern.matcher(htmlContent);
            
            if (generalMatcher.find()) {
                String apkFileName = generalMatcher.group(1);
                System.out.println("Found APK file (general pattern): " + apkFileName);
                
                Map<String, String> result = new HashMap<>();
                result.put("url", buildDirectoryUrl + apkFileName);
                result.put("fileName", apkFileName);
                return result;
            }
            
            System.err.println("No APK file found in directory");
            return null;
        } catch (Exception e) {
            System.err.println("Error searching for APK files: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Download a file from a URL to the specified path
     */
    private boolean downloadFile(String fileUrl, Path targetPath) {
        System.out.println("Downloading file from: " + fileUrl);
        System.out.println("Saving to: " + targetPath);
        
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);  // 10 seconds
            connection.setReadTimeout(120000);    // 2 minutes for larger files
            
            int responseCode = connection.getResponseCode();
            System.out.println("Download response code: " + responseCode);
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to download file. Response code: " + responseCode);
                return false;
            }
            
            int contentLength = connection.getContentLength();
            System.out.println("Content length: " + contentLength + " bytes");
            
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File successfully downloaded");
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error downloading file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Test the connection to the build server
     */
    @GetMapping("/test-build-server")
    @ResponseBody
    public Map<String, Object> testBuildServerConnection(@RequestParam(value = "type", required = false, defaultValue = "ios") String type) {
        Map<String, Object> response = new HashMap<>();
        String serverUrl = "ios".equalsIgnoreCase(type) ? IOS_BUILD_SERVER_BASE_URL : ANDROID_BUILD_SERVER_BASE_URL;
        response.put("url", serverUrl);
        response.put("type", type);
        
        try {
            System.out.println("Testing connection to build server: " + serverUrl);
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(30000);    // 30 seconds
            
            int responseCode = connection.getResponseCode();
            response.put("responseCode", responseCode);
            System.out.println("Build server test response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                response.put("connected", true);
                
                // Read some content to verify
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
                    StringBuilder content = new StringBuilder();
                    int lineCount = 0;
                    while (scanner.hasNextLine() && lineCount < 20) {
                        content.append(scanner.nextLine()).append("\n");
                        lineCount++;
                    }
                    
                    String htmlPreview = content.toString();
                    System.out.println("Content preview: " + htmlPreview);
                    
                    response.put("contentPreview", htmlPreview);
                    
                    // Try to find build directories
                    Pattern pattern = Pattern.compile("\\[DIR\\]\\s+(\\d+)/");
                    Matcher matcher = pattern.matcher(htmlPreview);
                    
                    List<Integer> buildNumbers = new ArrayList<>();
                    while (matcher.find()) {
                        try {
                            int buildNumber = Integer.parseInt(matcher.group(1));
                            buildNumbers.add(buildNumber);
                        } catch (NumberFormatException e) {
                            // Skip non-numeric
                        }
                    }
                    
                    response.put("buildNumbersFound", buildNumbers);
                    response.put("pattern", "\\[DIR\\]\\s+(\\d+)/");
                    response.put("patternMatched", !buildNumbers.isEmpty());
                }
            } else {
                response.put("connected", false);
                response.put("error", "Server returned non-OK response: " + responseCode);
            }
            
        } catch (Exception e) {
            response.put("connected", false);
            response.put("error", e.getMessage());
            response.put("exceptionType", e.getClass().getName());
            e.printStackTrace();
        }
        
        return response;
    }

    // Add this new endpoint to handle direct file downloads for compatibility
    @GetMapping("/{*path}")
    @ResponseBody
    public ResponseEntity<Resource> directFileDownload(@PathVariable String path) throws IOException {
        if (path == null || path.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Path file = Paths.get(ROOT_DIR, path);
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
} 