package com.example.local_cloud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;

@Controller
public class SonosDiscoveryController {

    @GetMapping("/discover-sonos")
    public String discoverSonos(Model model) {
        System.out.println("🛰️ Starting Sonos discovery...");
        List<Map<String, String>> devices = new ArrayList<>();
        Set<String> seenLocations = new HashSet<>();

        try {
            String searchMessage =
                    "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n";

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);

            DatagramPacket packet = new DatagramPacket(
                    searchMessage.getBytes(),
                    searchMessage.length(),
                    InetAddress.getByName("239.255.255.250"),
                    1900
            );
            socket.send(packet);

            byte[] buf = new byte[2048];
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 20000) {
                try {
                    DatagramPacket response = new DatagramPacket(buf, buf.length);
                    socket.receive(response);
                    String data = new String(response.getData(), 0, response.getLength());

                    String location = parseHeader(data, "LOCATION");
                    if (location != null && !seenLocations.contains(location)) {
                        seenLocations.add(location);
                        Map<String, String> info = fetchDeviceInfo(location);
                        if (info != null) {
                            devices.add(info);
                        } else {
                            System.out.println("⚠️ Skipped device at " + location);
                        }
                    }

                } catch (SocketTimeoutException e) {
                    break;
                }
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("📋 Devices found: " + devices.size());
        model.addAttribute("devices", devices);
        return "sonos_table";
    }

    private String parseHeader(String data, String header) {
        for (String line : data.split("\r\n")) {
            if (line.toLowerCase().startsWith(header.toLowerCase() + ":")) {
                return line.split(":", 2)[1].trim();
            }
        }
        return null;
    }

    private Map<String, String> fetchDeviceInfo(String location) {
        Map<String, String> map = new LinkedHashMap<>();

        try {
            URL url = new URL(location);
            String ip = url.getHost();
            map.put("IP", ip);
            System.out.println("📡 Probing: " + ip);

            // ✅ Ping with timeout
            long start = System.currentTimeMillis();
            boolean reachable = InetAddress.getByName(ip).isReachable(500);
            long latency = reachable ? System.currentTimeMillis() - start : -1;

            if (!reachable || latency > 1000) {
                System.out.println("⏱️ Too slow/unreachable: " + ip);
                return null;
            }

            map.put("Latency", latency + " ms");

            // ✅ Fetch /xml/device_description.xml
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.connect();

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();

            // MAC from serialNum
            String serialNum = xpath.evaluate("//*[local-name()='serialNum']", doc);
            if (serialNum != null && serialNum.contains(":")) {
                map.put("MAC Address", serialNum.split(":")[0]);
            } else {
                map.put("MAC Address", "Unknown");
            }

            // Room Name from roomName tag
            String roomName = xpath.evaluate("//*[local-name()='roomName']", doc);
            map.put("Room Name", (roomName != null && !roomName.isEmpty()) ? roomName : "Unknown");

            map.put("Model", xpath.evaluate("//*[local-name()='modelName']", doc));
            map.put("Software Version", xpath.evaluate("//*[local-name()='softwareVersion']", doc));
            map.put("Hardware Version", xpath.evaluate("//*[local-name()='hardwareVersion']", doc));

            System.out.println("✅ Device OK: " + ip + " → " + roomName);
            return map;

        } catch (Exception e) {
            System.err.println("❌ Skipping device at " + location + ": " + e.getMessage());
            return null;
        }
    }
}
