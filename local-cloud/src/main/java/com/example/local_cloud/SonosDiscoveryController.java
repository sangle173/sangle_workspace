package com.example.local_cloud;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.w3c.dom.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

@Controller
public class SonosDiscoveryController {

    private final Map<String, DeviceStatus> deviceStatusMap = new ConcurrentHashMap<>();

    @GetMapping("/sonos")
    public String sonosPage() {
        return "sonos";
    }

    @GetMapping("/stream-sonos")
    @ResponseBody
    public SseEmitter streamSonos() {
        SseEmitter emitter = new SseEmitter(25000L);

        new Thread(() -> {
            try {
                Set<String> seenLocations = new HashSet<>();

                String searchMessage = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 5\r\n" +
                        "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n";

                InetAddress wifiAddress = getWifiInterfaceAddress();
                DatagramSocket socket = new DatagramSocket(0, wifiAddress);
                socket.setReuseAddress(true);
                socket.setSoTimeout(1000);

                DatagramPacket packet = new DatagramPacket(
                        searchMessage.getBytes(),
                        searchMessage.length(),
                        InetAddress.getByName("239.255.255.250"),
                        1900);
                socket.send(packet);

                byte[] buf = new byte[2048];
                long endTime = System.currentTimeMillis() + 20000;

                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket response = new DatagramPacket(buf, buf.length);
                        socket.receive(response);
                        String data = new String(response.getData(), 0, response.getLength());

                        String location = parseHeader(data, "LOCATION");
                        if (location != null && seenLocations.add(location)) {
                            Map<String, String> info = fetchDeviceInfo(location);
                            if (info != null) {
                                emitter.send(SseEmitter.event().name("device").data(info));
                            }
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }

                socket.close();
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        return emitter;
    }

    @GetMapping("/ping-status")
    @ResponseBody
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

    private Map<String, String> fetchDeviceInfo(String location) {
        Map<String, String> map = new LinkedHashMap<>();

        try {
            URL url = new URL(location);
            String ip = url.getHost();
            map.put("IP", ip);
            System.out.println("📡 Probing: " + ip);

            // Add to ping map early
            deviceStatusMap.putIfAbsent(ip, new DeviceStatus(ip));

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.connect();

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();

            map.put("MAC Address", xpath.evaluate("//*[local-name()='serialNum']", doc));
            map.put("Room Name", xpath.evaluate("//*[local-name()='roomName']", doc));
            map.put("Model", xpath.evaluate("//*[local-name()='modelName']", doc));
            map.put("Software Version", xpath.evaluate("//*[local-name()='softwareVersion']", doc));
            map.put("Hardware Version", xpath.evaluate("//*[local-name()='hardwareVersion']", doc));

            // Extract icon
            String iconUrlPath = xpath
                    .evaluate("//*[local-name()='iconList']/*[local-name()='icon']/*[local-name()='url']", doc);
            if (iconUrlPath != null && !iconUrlPath.isEmpty()) {
                map.put("Image", "http://" + ip + ":1400" + iconUrlPath);
            } else {
                map.put("Image", "");
            }

            // Fetch HHID from /status/zp
            try {
                URL statusUrl = new URL("http://" + ip + ":1400/status/zp");
                HttpURLConnection conn = (HttpURLConnection) statusUrl.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);

                Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String html = scanner.hasNext() ? scanner.next() : "";

                String hhid = "—";
                Matcher matcher = Pattern.compile("<HouseholdControlID>(Sonos_[^<]+?)\\.").matcher(html);
                if (matcher.find()) {
                    hhid = matcher.group(1);
                    System.out.println("✅ HHID Found: " + hhid);
                } else {
                    System.out.println("❌ HHID not matched in: " + ip);
                }

                map.put("HHID", hhid);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to fetch HHID from " + ip + ": " + e.getMessage());
                map.put("HHID", "—");
            }

            System.out.println("✅ Device OK: " + ip + " → " + map.get("Room Name"));
            return map;

        } catch (Exception e) {
            System.err.println("❌ Skipping device at " + location + ": " + e.getMessage());
            return null;
        }
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
            if (!iface.isUp() || iface.isLoopback())
                continue;

            String name = iface.getName().toLowerCase();
            if (!(name.contains("wlan") || name.contains("wifi")))
                continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address) {
                    return addr;
                }
            }
        }
        throw new SocketException("Wi-Fi interface not found or no IPv4 address available.");
    }

    @PostConstruct
    public void startPingMonitor() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (DeviceStatus status : deviceStatusMap.values()) {
                try {
                    long start = System.currentTimeMillis();
                    boolean reachable = InetAddress.getByName(status.getIp()).isReachable(1000);
                    String latency = reachable ? (System.currentTimeMillis() - start) + " ms" : "lost";
                    status.setLatency(latency);
                } catch (IOException e) {
                    status.setLatency("lost");
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    // Add below existing methods in your @Controller
    @PostMapping("/sonos-action/reboot")
    @ResponseBody
    public String reboot(@RequestParam String ip) {
        try {
            // Step 1: Get CSRF token from GET /reboot page
            URL getUrl = new URL("http://" + ip + ":1400/reboot");
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");
            getConn.setConnectTimeout(2000);
            getConn.setReadTimeout(2000);

            Scanner scanner = new Scanner(getConn.getInputStream()).useDelimiter("\\A");
            String html = scanner.hasNext() ? scanner.next() : "";

            // Step 2: Extract token
            Pattern pattern = Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(html);
            if (!matcher.find()) {
                return "ERROR: CSRF token not found";
            }
            String csrfToken = matcher.group(1);
            System.out.println("🔐 CSRF token found: " + csrfToken);

            // Step 3: POST the token to /reboot
            URL postUrl = new URL("http://" + ip + ":1400/reboot");
            HttpURLConnection postConn = (HttpURLConnection) postUrl.openConnection();
            postConn.setRequestMethod("POST");
            postConn.setDoOutput(true);
            postConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            postConn.setConnectTimeout(2000);
            postConn.setReadTimeout(2000);

            String payload = "csrfToken=" + URLEncoder.encode(csrfToken, "UTF-8");
            postConn.getOutputStream().write(payload.getBytes());

            int responseCode = postConn.getResponseCode();
            return (responseCode == 200) ? "OK" : "FAILED (" + responseCode + ")";

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @PostMapping("/sonos-action/rename")
    @ResponseBody
    public String rename(@RequestParam String ip, @RequestParam String name, @RequestParam String current) {
        try {
            String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" +
                    "<u:SetRoomName xmlns:u=\"urn:schemas-upnp-org:service:DeviceProperties:1\">" +
                    "<DesiredRoomName>" + name + "</DesiredRoomName>" +
                    "<CurrentRoomName>" + current + "</CurrentRoomName>" +
                    "</u:SetRoomName>" +
                    "</s:Body></s:Envelope>";

            URL url = new URL("http://" + ip + ":1400/DeviceProperties/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:DeviceProperties:1#SetRoomName\"");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.getOutputStream().write(body.getBytes());

            int response = conn.getResponseCode();
            return (response == 200) ? "OK" : "FAILED (" + response + ")";

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @PostMapping("/sonos-action/update")
    @ResponseBody
    public String beginSoftwareUpdate(@RequestParam String ip, @RequestParam String url) {
        try {
            String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" +
                    "<u:BeginSoftwareUpdate xmlns:u=\"urn:schemas-upnp-org:service:DeviceProperties:1\">" +
                    "<UpdateURL>" + url + "</UpdateURL>" +
                    "<Flags>9</Flags>" +
                    "</u:BeginSoftwareUpdate>" +
                    "</s:Body></s:Envelope>";

            URL controlUrl = new URL("http://" + ip + ":1400/DeviceProperties/Control");
            HttpURLConnection conn = (HttpURLConnection) controlUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION",
                    "\"urn:schemas-upnp-org:service:DeviceProperties:1#BeginSoftwareUpdate\"");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            conn.getOutputStream().write(body.getBytes());
            int code = conn.getResponseCode();
            return (code == 200) ? "Update triggered." : "FAILED (" + code + ")";

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // Helper class for tracking ping info
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
