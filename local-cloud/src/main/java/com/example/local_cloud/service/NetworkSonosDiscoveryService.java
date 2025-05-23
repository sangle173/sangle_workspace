package com.example.local_cloud.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import java.util.function.Consumer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NetworkSonosDiscoveryService {
    private static final String DEVICES_JSON = "/opt/lampp/htdocs/sangle_workspace/local-cloud/sonos_devices.json";

    private String toNetworkAddress(String ipWithCidr) {
        // Example: 192.168.137.115/24 -> 192.168.137.0/24
        if (ipWithCidr == null || !ipWithCidr.contains("/")) return ipWithCidr;
        try {
            String[] parts = ipWithCidr.split("/");
            String[] octets = parts[0].split("\\.");
            int prefix = Integer.parseInt(parts[1]);
            int ip = (Integer.parseInt(octets[0]) << 24) |
                     (Integer.parseInt(octets[1]) << 16) |
                     (Integer.parseInt(octets[2]) << 8) |
                     (Integer.parseInt(octets[3]));
            int mask = 0xFFFFFFFF << (32 - prefix);
            int network = ip & mask;
            String networkAddr = String.format("%d.%d.%d.%d/%d",
                (network >> 24) & 0xFF,
                (network >> 16) & 0xFF,
                (network >> 8) & 0xFF,
                network & 0xFF,
                prefix);
            return networkAddr;
        } catch (Exception e) {
            return ipWithCidr;
        }
    }

    public List<Map<String, String>> getAvailableWifiNetworks() {
        List<Map<String, String>> networks = new ArrayList<>();
        Set<String> activeSsids = new HashSet<>();
        Map<String, String> activeSubnets = new HashMap<>();
        try {
            // Get active WiFi connections and their devices
            Process activeProc = Runtime.getRuntime().exec("nmcli -t -f NAME,DEVICE,TYPE connection show --active");
            BufferedReader activeReader = new BufferedReader(new InputStreamReader(activeProc.getInputStream()));
            String activeLine;
            while ((activeLine = activeReader.readLine()) != null) {
                String[] parts = activeLine.split(":");
                if (parts.length >= 3 && parts[2].equals("802-11-wireless")) {
                    String ssid = parts[0];
                    String device = parts[1];
                    activeSsids.add(ssid);
                    // Get subnet for this device
                    try {
                        Process ipProc = Runtime.getRuntime().exec("nmcli -t -f IP4.ADDRESS dev show " + device);
                        BufferedReader ipReader = new BufferedReader(new InputStreamReader(ipProc.getInputStream()));
                        String ipLine;
                        while ((ipLine = ipReader.readLine()) != null) {
                            if (ipLine.contains("/")) {
                                String[] ipParts = ipLine.split(":");
                                if (ipParts.length == 2) {
                                    activeSubnets.put(ssid, ipParts[1].trim());
                                    break;
                                }
                            }
                        }
                        ipReader.close();
                    } catch (Exception e) {
                        // Ignore, just leave subnet empty
                    }
                }
            }
            activeReader.close();

            // List all saved WiFi connections
            Process proc = Runtime.getRuntime().exec("nmcli -t -f NAME,TYPE connection show");
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[1].equals("802-11-wireless")) {
                    String ssid = parts[0];
                    String subnet = activeSubnets.getOrDefault(ssid, "");
                    if (!subnet.isEmpty()) subnet = toNetworkAddress(subnet);
                    boolean active = activeSsids.contains(ssid);
                    Map<String, String> net = new HashMap<>();
                    net.put("ssid", ssid);
                    net.put("subnet", subnet);
                    net.put("active", Boolean.toString(active));
                    networks.add(net);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return networks;
    }

    public Map<String, Object> scanAllNetworks() {
        // SSDP/UPnP multicast discovery (like SonosActionController)
        Set<String> seenLocations = new HashSet<>();
        List<Map<String, String>> allDevices = new ArrayList<>();
        try {
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
            long endTime = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < endTime) {
                try {
                    DatagramPacket response = new DatagramPacket(buf, buf.length);
                    socket.receive(response);
                    String data = new String(response.getData(), 0, response.getLength());
                    String location = parseHeader(data, "LOCATION");
                    if (location != null && seenLocations.add(location)) {
                        Map<String, String> info = fetchDeviceInfo(location);
                        if (info != null) {
                            allDevices.add(info);
                        }
                    }
                } catch (SocketTimeoutException ignored) {}
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Save/append to JSON file
        saveDevicesToJson(allDevices);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Scan complete. " + allDevices.size() + " devices found.");
        return result;
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
        List<String> preferred = Arrays.asList("wlan", "wifi", "wl", "en", "eth");
        InetAddress fallback = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual())
                continue;
            String name = iface.getName().toLowerCase();
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    if (preferred.stream().anyMatch(name::contains)) {
                        return addr;
                    }
                    if (fallback == null) {
                        fallback = addr;
                    }
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new SocketException("No usable network interface with IPv4 address found.");
    }

    private Map<String, String> fetchDeviceInfo(String location) {
        try {
            URL url = new URL(location);
            String ip = url.getHost();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();
            Map<String, String> map = new LinkedHashMap<>();
            map.put("ip", ip);
            map.put("roomName", xpath.evaluate("//*[local-name()='roomName']", doc));
            map.put("friendlyName", xpath.evaluate("//*[local-name()='friendlyName']", doc));
            String udn = xpath.evaluate("//*[local-name()='UDN']", doc);
            if (udn != null && udn.startsWith("uuid:")) udn = udn.substring(5);
            map.put("UDN", udn);
            map.put("MACAddress", xpath.evaluate("//*[local-name()='MACAddress']", doc));
            map.put("serialNum", xpath.evaluate("//*[local-name()='serialNum']", doc));
            map.put("softwareVersion", xpath.evaluate("//*[local-name()='softwareVersion']", doc));
            map.put("minCompatibleVersion", xpath.evaluate("//*[local-name()='minCompatibleVersion']", doc));
            map.put("displayVersion", xpath.evaluate("//*[local-name()='displayVersion']", doc));
            map.put("hardwareVersion", xpath.evaluate("//*[local-name()='hardwareVersion']", doc));
            map.put("modelNumber", xpath.evaluate("//*[local-name()='modelNumber']", doc));
            map.put("modelName", xpath.evaluate("//*[local-name()='modelName']", doc));
            map.put("modelDescription", xpath.evaluate("//*[local-name()='modelDescription']", doc));
            map.put("manufacturer", xpath.evaluate("//*[local-name()='manufacturer']", doc));
            map.put("manufacturerURL", xpath.evaluate("//*[local-name()='manufacturerURL']", doc));
            map.put("marketVersion", xpath.evaluate("//*[local-name()='displayVersion']", doc)); // alias for displayVersion
            map.put("statusUrl", "http://" + ip + ":1400/status");
            // iconList (concatenate all icon URLs)
            StringBuilder icons = new StringBuilder();
            try {
                org.w3c.dom.NodeList iconNodes = (org.w3c.dom.NodeList) xpath.evaluate("//*[local-name()='iconList']/*[local-name()='icon']/*[local-name()='url']", doc, javax.xml.xpath.XPathConstants.NODESET);
                for (int i = 0; i < iconNodes.getLength(); i++) {
                    if (i > 0) icons.append(",");
                    icons.append(iconNodes.item(i).getTextContent());
                }
            } catch (Exception ignore) {}
            map.put("iconList", icons.toString());
            // Add HHID if you have a way to fetch it, else blank
            map.put("HHID", "");
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Auto-connects to each saved WiFi network in sequence, scans for Sonos devices, and saves results.
     * Only works if the system supports nmcli and the user has permissions.
     */
    public Map<String, Object> autoScanAllWifiNetworks() {
        List<Map<String, String>> networks = getAvailableWifiNetworks();
        List<Map<String, String>> allDevices = new ArrayList<>();
        int scanned = 0, failed = 0;
        String scanDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        for (Map<String, String> net : networks) {
            String ssid = net.get("ssid");
            try {
                // Connect to the network
                Process connectProc = Runtime.getRuntime().exec(new String[]{"nmcli", "connection", "up", ssid});
                int exit = connectProc.waitFor();
                if (exit != 0) {
                    failed++;
                    continue;
                }
                // Wait for connection to be established
                Thread.sleep(3500); // May need to adjust for your environment
                // Scan using SSDP/UPnP
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
                long endTime = System.currentTimeMillis() + 5000;
                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket response = new DatagramPacket(buf, buf.length);
                        socket.receive(response);
                        String data = new String(response.getData(), 0, response.getLength());
                        String location = parseHeader(data, "LOCATION");
                        if (location != null && seenLocations.add(location)) {
                            Map<String, String> info = fetchDeviceInfo(location);
                            if (info != null) {
                                info.put("network", ssid);
                                info.put("scanDateTime", scanDateTime);
                                allDevices.add(info);
                            }
                        }
                    } catch (SocketTimeoutException ignored) {}
                }
                socket.close();
                scanned++;
            } catch (Exception e) {
                failed++;
            }
        }
        // Save/append to JSON file
        saveDevicesToJson(allDevices);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Auto-scan complete. " + scanned + " networks scanned, " + allDevices.size() + " devices found. " + (failed > 0 ? (failed + " failed.") : ""));
        return result;
    }

    /**
     * Auto-connects to each saved WiFi network in sequence, scans for Sonos devices, and saves results.
     * Sends progress updates to the provided Consumer.
     */
    public Map<String, Object> autoScanAllWifiNetworksWithProgress(Consumer<String> progressCallback) {
        List<Map<String, String>> networks = getAvailableWifiNetworks();
        List<Map<String, String>> allDevices = new ArrayList<>();
        int scanned = 0, failed = 0;
        String scanDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        for (Map<String, String> net : networks) {
            String ssid = net.get("ssid");
            try {
                progressCallback.accept("Connecting to network '" + ssid + "'...");
                // Connect to the network
                Process connectProc = Runtime.getRuntime().exec(new String[]{"nmcli", "connection", "up", ssid});
                int exit = connectProc.waitFor();
                if (exit != 0) {
                    progressCallback.accept("Failed to connect to '" + ssid + "'. Skipping.");
                    failed++;
                    continue;
                }
                progressCallback.accept("Connected to '" + ssid + "'. Waiting for network to settle...");
                // Wait for connection to be established
                Thread.sleep(3500); // May need to adjust for your environment
                progressCallback.accept("Scanning for Sonos devices in '" + ssid + "'...");
                // Scan using SSDP/UPnP
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
                long endTime = System.currentTimeMillis() + 5000;
                int found = 0;
                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket response = new DatagramPacket(buf, buf.length);
                        socket.receive(response);
                        String data = new String(response.getData(), 0, response.getLength());
                        String location = parseHeader(data, "LOCATION");
                        if (location != null && seenLocations.add(location)) {
                            Map<String, String> info = fetchDeviceInfo(location);
                            if (info != null) {
                                info.put("network", ssid);
                                info.put("scanDateTime", scanDateTime);
                                allDevices.add(info);
                                found++;
                                progressCallback.accept("Discovered device: " + info.getOrDefault("roomName", info.getOrDefault("ip", "Unknown")));
                            }
                        }
                    } catch (SocketTimeoutException ignored) {}
                }
                socket.close();
                progressCallback.accept("Scan complete for '" + ssid + "'. " + found + " device(s) found.");
                scanned++;
            } catch (Exception e) {
                progressCallback.accept("Error scanning '" + ssid + "': " + e.getMessage());
                failed++;
            }
        }
        // Save/append to JSON file
        saveDevicesToJson(allDevices);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Auto-scan complete. " + scanned + " networks scanned, " + allDevices.size() + " devices found. " + (failed > 0 ? (failed + " failed.") : ""));
        progressCallback.accept(result.get("message").toString());
        return result;
    }

    private void saveDevicesToJson(List<Map<String, String>> newDevices) {
        try {
            File file = new File(DEVICES_JSON);
            List<Map<String, String>> all = new ArrayList<>();
            if (file.exists()) {
                all.addAll(readDevicesFromJson());
            }
            all.addAll(newDevices);
            // Remove duplicates by IP
            Map<String, Map<String, String>> unique = new LinkedHashMap<>();
            for (Map<String, String> d : all) unique.put(d.get("ip"), d);
            all = new ArrayList<>(unique.values());
            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(all));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> readDevicesFromJson() {
        try {
            File file = new File(DEVICES_JSON);
            if (!file.exists()) return new ArrayList<>();
            return (List<Map<String, String>>) new com.fasterxml.jackson.databind.ObjectMapper().readValue(file, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Map<String, List<Map<String, String>>> getDevicesByNetwork() {
        List<Map<String, String>> all = readDevicesFromJson();
        Map<String, List<Map<String, String>>> grouped = new HashMap<>();
        for (Map<String, String> d : all) {
            String net = d.getOrDefault("network", "Unknown");
            grouped.computeIfAbsent(net, k -> new ArrayList<>()).add(d);
        }
        return grouped;
    }
}
