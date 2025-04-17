package com.example.local_cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
import org.w3c.dom.Document;

@RestController
@RequestMapping("/sonos-action")
public class SonosActionController {

    private final SonosService sonosService;
    private final Map<String, DeviceStatus> deviceStatusMap = new ConcurrentHashMap<>();

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

        new Thread(() -> {
            try {
                Set<String> seenLocations = new HashSet<>();

                String searchMessage = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 3\r\n" +
                        "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n";

                InetAddress wifiAddress = getWifiInterfaceAddress();
                DatagramSocket socket = new DatagramSocket(0, wifiAddress);
                socket.setSoTimeout(1000);
                socket.send(new DatagramPacket(searchMessage.getBytes(), searchMessage.length(),
                        InetAddress.getByName("239.255.255.250"), 1900));

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
                            if (info != null)
                                emitter.send(SseEmitter.event().name("device").data(info));
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

    private Map<String, String> fetchDeviceInfo(String location) {
        try {
            URL url = new URL(location);
            String ip = url.getHost();

            deviceStatusMap.putIfAbsent(ip, new DeviceStatus(ip));

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

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

            try {
                URL statusUrl = new URL("http://" + ip + ":1400/status/zp");
                HttpURLConnection statusConn = (HttpURLConnection) statusUrl.openConnection();
                statusConn.setConnectTimeout(1000);
                statusConn.setReadTimeout(1000);

                Scanner scanner = new Scanner(statusConn.getInputStream()).useDelimiter("\\A");
                String html = scanner.hasNext() ? scanner.next() : "";

                Matcher matcher = Pattern.compile("<HouseholdControlID>(Sonos_[^<]+?)\\.").matcher(html);
                map.put("HHID", matcher.find() ? matcher.group(1) : "—");

            } catch (Exception e) {
                map.put("HHID", "—");
            }

            return map;
        } catch (Exception e) {
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
                if (addr instanceof Inet4Address)
                    return addr;
            }
        }
        throw new SocketException("Wi-Fi interface not found or no IPv4 address available.");
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
