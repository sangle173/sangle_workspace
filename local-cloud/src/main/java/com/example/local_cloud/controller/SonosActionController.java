package com.example.local_cloud.controller;

import com.example.local_cloud.dto.BulkActionRequest;
import com.example.local_cloud.service.SonosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.w3c.dom.Document;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/sonos-action")
public class SonosActionController {

    private final SonosService sonosService;
    private final Map<String, DeviceStatus> deviceStatusMap = new ConcurrentHashMap<>();

    // Device info cache: IP -> CachedDeviceInfo
    private static class CachedDeviceInfo {
        final Map<String, String> info;
        final long timestamp;
        CachedDeviceInfo(Map<String, String> info, long timestamp) {
            this.info = info;
            this.timestamp = timestamp;
        }
    }
    private final ConcurrentHashMap<String, CachedDeviceInfo> deviceInfoCache = new ConcurrentHashMap<>();
    private static final long DEVICE_INFO_CACHE_TTL_MS = 2 * 60 * 1000; // 2 minutes

    @Autowired
    public SonosActionController(SonosService sonosService) {
        this.sonosService = sonosService;
    }

    @PostMapping("/update")
    public String updateSonosDevice(@RequestParam String ip, @RequestParam String url) {
        return sonosService.sendSoftwareUpdate(ip, url);
    }

    @PostMapping("/reboot")
    public String reboot(@RequestParam String ip) {
        return sonosService.rebootDevice(ip);
    }

    @PostMapping("/rename")
    public String rename(@RequestParam String ip, @RequestParam String name, @RequestParam String current) {
        return sonosService.renameDevice(ip, name, current);
    }

    @GetMapping("/ping-status")
    public List<Map<String, String>> getPingStatus() {
        List<Map<String, String>> list = new ArrayList<>();
        for (DeviceStatus status : deviceStatusMap.values()) {
            Map<String, String> entry = new HashMap<>();
            entry.put("ip", status.getIp());
            entry.put("latency", status.getLatency());
            list.add(entry);
        }
        return list;
    }

    @PostMapping("/submit-diagnostics")
    public String submitDiagnostics(
            @RequestParam String ip,
            @RequestParam(defaultValue = "1") int includeControllers,
            @RequestParam(defaultValue = "user") String type) {
        return sonosService.submitDiagnostics(ip, includeControllers == 1, type);
    }

    @GetMapping("/stream-sonos")
    public SseEmitter streamSonos() {
        SseEmitter emitter = new SseEmitter(25000L);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Set<String> seenLocations = new HashSet<>();
                String searchMessage = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 3\r\n" +
                        "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n";

                InetAddress localAddress = getWifiInterfaceAddress();
                DatagramSocket socket = new DatagramSocket(0, localAddress);
                socket.setSoTimeout(1000);
                socket.send(new DatagramPacket(searchMessage.getBytes(), searchMessage.length(),
                        InetAddress.getByName("239.255.255.250"), 1900));

                byte[] buf = new byte[2048];
                long endTime = System.currentTimeMillis() + 10000;

                ExecutorService pool = Executors.newFixedThreadPool(24);
                List<Future<?>> futures = new ArrayList<>();

                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket response = new DatagramPacket(buf, buf.length);
                        socket.receive(response);
                        String data = new String(response.getData(), 0, response.getLength());
                        String location = parseHeader(data, "LOCATION");

                        if (location != null && seenLocations.add(location)) {
                            futures.add(pool.submit(() -> {
                                Map<String, String> info = fetchDeviceInfoWithTimeout(location, 700, 700);
                                if (info != null) {
                                    try {
                                        emitter.send(SseEmitter.event().name("device").data(info));
                                    } catch (IOException ignored) {
                                    }
                                }
                            }));
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }

                for (Future<?> future : futures) {
                    try {
                        future.get(2, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                }
                pool.shutdown();
                socket.close();
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return emitter;
    }

    @PostMapping("/bulk")
    public String performBulkAction(@RequestBody BulkActionRequest request) {
        List<String> results = new ArrayList<>();

        for (String ip : request.getDevices()) {
            String msg;
            switch (request.getAction()) {
                case "reboot":
                    msg = sonosService.rebootDevice(ip);
                    break;
                case "submit-diagnostics":
                    msg = sonosService.submitDiagnostics(ip, true, "user");
                    break;
                case "update": {
                    // Bulk update logic: fetch device info, extract UPD, build update URL
                    try {
                        // Fetch device info from description.xml
                        String location = "http://" + ip + ":1400/xml/device_description.xml";
                        Map<String, String> info = fetchDeviceInfo(location);
                        if (info == null) {
                            msg = "❌ Could not fetch device info";
                            break;
                        }
                        String hwv = info.getOrDefault("Hardware Version", "");
                        // Extract UPD number from hardware version (e.g., 1.49.1.2 -> 49)
                        String upd = "";
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^1\\.(\\d+)\\.").matcher(hwv);
                        if (m.find()) {
                            upd = m.group(1);
                        }
                        if (upd.isEmpty()) {
                            msg = "❌ Could not extract UPD from hardware version: " + hwv;
                            break;
                        }
                        String updateFile = "90.0-65020-1-" + upd + ".upd";
                        String baseUrl = request.getBaseUrl();
                        if (baseUrl == null || baseUrl.isEmpty()) {
                            msg = "❌ No base URL provided";
                            break;
                        }
                        if (!baseUrl.endsWith("/")) baseUrl += "/";
                        String updateUrl = baseUrl + updateFile;
                        msg = sonosService.sendSoftwareUpdate(ip, updateUrl);
                    } catch (Exception e) {
                        msg = "❌ Update error: " + e.getMessage();
                    }
                    break;
                }
                default:
                    msg = "❓ Unknown action: " + request.getAction();
                    break;
            }
            results.add("<b>" + ip + "</b>: " + msg);
        }

        return "<div class='text-success'>✅ Action <b>" + request.getAction() + "</b> sent to "
                + results.size() + " device(s).</div><ul><li>"
                + String.join("</li><li>", results) + "</li></ul>";
    }

    @PostConstruct
    public void startDeviceInfoRefresher() {
        ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();
        refresher.scheduleAtFixedRate(() -> {
            try {
                Set<String> ips = new HashSet<>(deviceInfoCache.keySet());
                ips.parallelStream().forEach(ip -> {
                    try {
                        fetchDeviceInfo("http://" + ip + ":1400/xml/device_description.xml");
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }, 30, 30, TimeUnit.SECONDS);
    }

    private Map<String, String> fetchDeviceInfo(String location) {
        try {
            URL url = new URL(location);
            String ip = url.getHost();

            // Check cache first
            CachedDeviceInfo cached = deviceInfoCache.get(ip);
            long now = System.currentTimeMillis();
            if (cached != null && (now - cached.timestamp) < DEVICE_INFO_CACHE_TTL_MS) {
                return cached.info;
            }

            deviceStatusMap.putIfAbsent(ip, new DeviceStatus(ip));

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();

            Map<String, String> map = new LinkedHashMap<>();
            map.put("IP", ip);
            map.put("MAC Address", xpath.evaluate("//*[local-name()='serialNum']", doc));
            map.put("Room Name", xpath.evaluate("//*[local-name()='roomName']", doc));
            map.put("Model", xpath.evaluate("//*[local-name()='modelName']", doc));
            map.put("Software Version", xpath.evaluate("//*[local-name()='softwareVersion']", doc));
            map.put("Hardware Version", xpath.evaluate("//*[local-name()='hardwareVersion']", doc));

            String iconPath = xpath
                    .evaluate("//*[local-name()='iconList']/*[local-name()='icon']/*[local-name()='url']", doc);
            map.put("Image", (iconPath != null && !iconPath.isEmpty()) ? "http://" + ip + ":1400" + iconPath : "");

            // Get HHID using SOAP DeviceProperties/GetHouseholdID
            try {
                String hhid = getHouseholdIDViaDeviceProperties(ip);
                map.put("HHID", hhid != null ? hhid : "—");
            } catch (Exception e) {
                map.put("HHID", "—");
            }

            // Update cache
            deviceInfoCache.put(ip, new CachedDeviceInfo(map, now));

            return map;
        } catch (Exception e) {
            return null;
        }
    }

    // Helper to get HHID via DeviceProperties SOAP
    private String getHouseholdIDViaDeviceProperties(String ip) throws Exception {
        String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:GetHouseholdID xmlns:u=\"urn:schemas-upnp-org:service:DeviceProperties:1\"/>" +
                "</s:Body>" +
                "</s:Envelope>";
        URL url = new URL("http://" + ip + ":1400/DeviceProperties/Control");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:DeviceProperties:1#GetHouseholdID\"");
        conn.getOutputStream().write(soapBody.getBytes());
        conn.getOutputStream().flush();
        conn.getOutputStream().close();
        if (conn.getResponseCode() != 200) throw new Exception("SOAP HHID error: " + conn.getResponseCode());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
        XPath xpath = XPathFactory.newInstance().newXPath();
        String hhid = xpath.evaluate("//*[local-name()='CurrentHouseholdID']", doc);
        return (hhid != null && !hhid.isEmpty()) ? hhid : null;
    }

    private String parseHeader(String data, String header) {
        for (String line : data.split("\r\n")) {
            if (line.toLowerCase().startsWith(header.toLowerCase() + ":")) {
                return line.split(":", 2)[1].trim();
            }
        }
        return null;
    }

    private InetAddress getWifiInterfaceAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual())
                continue;

            String name = iface.getName().toLowerCase();
            if (name.contains("eth") || name.contains("en") || name.contains("wlan") || name.contains("wifi")) {
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr;
                    }
                }
            }
        }
        throw new SocketException("❌ No usable network interface with IPv4 address found.");
    }

    // Enhanced ping: optimized for speed, lower timeout, and process cleanup
    private String pingDevice(String ip) {
        String os = System.getProperty("os.name").toLowerCase();
        int timeoutMs = 200; // Lowered timeout for even faster response
        try {
            if (os.contains("linux")) {
                Process proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/ping", "-c", "1", "-W", "1", ip});
                boolean finished = proc.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!finished) {
                    proc.destroyForcibly();
                    return "lost";
                }
                try (Scanner s = new Scanner(proc.getInputStream())) {
                    String latency = "lost";
                    while (s.hasNextLine()) {
                        String line = s.nextLine();
                        if (line.contains("time=")) {
                            int idx = line.indexOf("time=");
                            int end = line.indexOf(" ms", idx);
                            if (idx != -1 && end != -1) {
                                latency = line.substring(idx + 5, end) + " ms";
                                break;
                            }
                        }
                    }
                    return latency;
                }
            } else if (os.contains("win")) {
                Process proc = Runtime.getRuntime().exec(new String[]{"ping", "-n", "1", "-w", String.valueOf(timeoutMs), ip});
                boolean finished = proc.waitFor(timeoutMs + 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!finished) {
                    proc.destroyForcibly();
                    return "lost";
                }
                try (Scanner s = new Scanner(proc.getInputStream())) {
                    String latency = "lost";
                    while (s.hasNextLine()) {
                        String line = s.nextLine();
                        if (line.contains("Average = ")) {
                            int idx = line.indexOf("Average = ");
                            int end = line.indexOf("ms", idx);
                            if (idx != -1 && end != -1) {
                                latency = line.substring(idx + 10, end).trim() + " ms";
                                break;
                            }
                        }
                    }
                    return latency;
                }
            } else {
                // Fallback to Java's isReachable
                long start = System.currentTimeMillis();
                boolean reachable = InetAddress.getByName(ip).isReachable(timeoutMs);
                return reachable ? (System.currentTimeMillis() - start) + " ms" : "lost";
            }
        } catch (Exception e) {
            return "lost";
        }
    }

    @PostConstruct
    public void startPingMonitor() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(24); // Increased pool size

        scheduler.scheduleAtFixedRate(() -> {
            deviceStatusMap.values().parallelStream().forEach(status -> {
                try {
                    status.setLatency(pingDevice(status.getIp()));
                } catch (Exception e) {
                    status.setLatency("lost");
                }
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    @GetMapping("/get-system-string")
    public String getSystemString(@RequestParam String ip, @RequestParam(defaultValue = "OnlineUpdateBaseURL") String variableName) {
        try {
            String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" +
                    "<u:GetString xmlns:u=\"urn:schemas-upnp-org:service:SystemProperties:1\">" +
                    "<VariableName>" + variableName + "</VariableName>" +
                    "</u:GetString>" +
                    "</s:Body>" +
                    "</s:Envelope>";
            URL url = new URL("http://" + ip + ":1400/SystemProperties/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:SystemProperties:1#GetString\"");
            conn.getOutputStream().write(soapBody.getBytes());
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            if (conn.getResponseCode() != 200) throw new Exception("SOAP GetString error: " + conn.getResponseCode());
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();
            String value = xpath.evaluate("//*[local-name()='StringValue']", doc);
            return value != null ? value : "";
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    @PostMapping("/set-system-string")
    public String setSystemString(@RequestParam String ip,
                                  @RequestParam(defaultValue = "OnlineUpdateBaseURL") String variableName,
                                  @RequestParam String stringValue) {
        try {
            String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" +
                    "<u:SetString xmlns:u=\"urn:schemas-upnp-org:service:SystemProperties:1\">" +
                    "<VariableName>" + variableName + "</VariableName>" +
                    "<StringValue>" + stringValue + "</StringValue>" +
                    "</u:SetString>" +
                    "</s:Body>" +
                    "</s:Envelope>";
            URL url = new URL("http://" + ip + ":1400/SystemProperties/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:SystemProperties:1#SetString\"");
            conn.getOutputStream().write(soapBody.getBytes());
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            if (conn.getResponseCode() != 200) throw new Exception("SOAP SetString error: " + conn.getResponseCode());
            // Success if 200 OK
            return "✅ SetString succeeded.";
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    // Helper to fetch device info with custom timeouts
    private Map<String, String> fetchDeviceInfoWithTimeout(String location, int connectTimeoutMs, int readTimeoutMs) {
        try {
            URL url = new URL(location);
            String ip = url.getHost();

            // Check cache first
            CachedDeviceInfo cached = deviceInfoCache.get(ip);
            long now = System.currentTimeMillis();
            if (cached != null && (now - cached.timestamp) < DEVICE_INFO_CACHE_TTL_MS) {
                return cached.info;
            }

            deviceStatusMap.putIfAbsent(ip, new DeviceStatus(ip));

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();

            Map<String, String> map = new LinkedHashMap<>();
            map.put("IP", ip);
            map.put("MAC Address", xpath.evaluate("//*[local-name()='serialNum']", doc));
            map.put("Room Name", xpath.evaluate("//*[local-name()='roomName']", doc));
            map.put("Model", xpath.evaluate("//*[local-name()='modelName']", doc));
            map.put("Software Version", xpath.evaluate("//*[local-name()='softwareVersion']", doc));
            map.put("Hardware Version", xpath.evaluate("//*[local-name()='hardwareVersion']", doc));

            String iconPath = xpath
                    .evaluate("//*[local-name()='iconList']/*[local-name()='icon']/*[local-name()='url']", doc);
            map.put("Image", (iconPath != null && !iconPath.isEmpty()) ? "http://" + ip + ":1400" + iconPath : "");

            // Get HHID using SOAP DeviceProperties/GetHouseholdID
            try {
                String hhid = getHouseholdIDViaDeviceProperties(ip);
                map.put("HHID", hhid != null ? hhid : "—");
            } catch (Exception e) {
                map.put("HHID", "—");
            }

            // Update cache
            deviceInfoCache.put(ip, new CachedDeviceInfo(map, now));

            return map;
        } catch (Exception e) {
            return null;
        }
    }

    public static class DeviceStatus {
        private final String ip;
        private volatile String latency = "ping...";

        public DeviceStatus(String ip) {
            this.ip = ip;
        }

        public String getIp() {
            return ip;
        }

        public String getLatency() {
            return latency;
        }

        public void setLatency(String latency) {
            this.latency = latency;
        }
    }
}
