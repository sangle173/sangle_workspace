package com.example.local_cloud.service;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.lang3.StringEscapeUtils;
import java.util.Map;

@Service
public class SonosService {

    public String sendSoftwareUpdate(String ip, String updUrl) {
        try {
            System.out.println("🛰️ BeginSoftwareUpdate via ZoneGroupTopology → " + ip);
            System.out.println("🔗 UpdateURL: " + updUrl);
    
            String soapBody =
                "<u:BeginSoftwareUpdate xmlns:u=\"urn:schemas-upnp-org:service:ZoneGroupTopology:1\">" +
                    "<UpdateURL>" + updUrl + "</UpdateURL>" +
                    "<Flags>9</Flags>" +
                    "<ExtraOptions></ExtraOptions>" +
                "</u:BeginSoftwareUpdate>";
    
            String soapEnvelope =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" + soapBody + "</s:Body>" +
                "</s:Envelope>";
    
            URL url = new URL("http://" + ip + ":1400/ZoneGroupTopology/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:ZoneGroupTopology:1#BeginSoftwareUpdate\"");
    
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
            }
    
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return "✅ Update command sent successfully via ZoneGroupTopology.";
            } else {
                try (Scanner errorScanner = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                    String errorBody = errorScanner.hasNext() ? errorScanner.next() : "Unknown error";
                    return "❌ Failed (" + responseCode + "):\n" + errorBody;
                }
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Exception: " + e.getMessage();
        }
    }
    
    public String rebootDevice(String ip) {
        try {
            // GET CSRF token from /reboot page
            URL getUrl = new URL("http://" + ip + ":1400/reboot");
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");

            try (Scanner scanner = new Scanner(getConn.getInputStream()).useDelimiter("\\A")) {
                String html = scanner.hasNext() ? scanner.next() : "";

                Matcher matcher = Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(html);
                if (!matcher.find()) return "❌ CSRF token not found";

                String token = matcher.group(1);

                // POST token back to /reboot
                URL postUrl = new URL("http://" + ip + ":1400/reboot");
                HttpURLConnection postConn = (HttpURLConnection) postUrl.openConnection();
                postConn.setRequestMethod("POST");
                postConn.setDoOutput(true);
                postConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String payload = "csrfToken=" + URLEncoder.encode(token, "UTF-8");
                postConn.getOutputStream().write(payload.getBytes());

                return postConn.getResponseCode() == 200 ? "✅ Rebooted" : "❌ Failed (" + postConn.getResponseCode() + ")";
            }
        } catch (Exception e) {
            return "❌ Reboot error: " + e.getMessage();
        }
    }

    public String renameDevice(String ip, String newName, String currentName) {
        try {
            String body =
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" +
                    "<u:SetRoomName xmlns:u=\"urn:schemas-upnp-org:service:DeviceProperties:1\">" +
                    "<DesiredRoomName>" + newName + "</DesiredRoomName>" +
                    "<CurrentRoomName>" + currentName + "</CurrentRoomName>" +
                    "</u:SetRoomName>" +
                    "</s:Body></s:Envelope>";

            HttpURLConnection conn = (HttpURLConnection) new URL("http://" + ip + ":1400/DeviceProperties/Control").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:DeviceProperties:1#SetRoomName\"");

            conn.getOutputStream().write(body.getBytes());

            return conn.getResponseCode() == 200 ? "✅ Renamed" : "❌ Rename failed (" + conn.getResponseCode() + ")";
        } catch (Exception e) {
            return "❌ Rename error: " + e.getMessage();
        }
    }

    public String submitDiagnostics(String ip, boolean includeControllers, String type) {
        try {
            String soapBody =
                "<u:SubmitDiagnostics xmlns:u=\"urn:schemas-upnp-org:service:ZoneGroupTopology:1\">" +
                    "<IncludeControllers>" + (includeControllers ? "1" : "0") + "</IncludeControllers>" +
                    "<Type>" + type + "</Type>" +
                "</u:SubmitDiagnostics>";
    
            String soapEnvelope =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                    "<s:Body>" + soapBody + "</s:Body>" +
                "</s:Envelope>";
    
            URL url = new URL("http://" + ip + ":1400/ZoneGroupTopology/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:ZoneGroupTopology:1#SubmitDiagnostics\"");
    
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
            }
    
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                    String body = scanner.hasNext() ? scanner.next() : "";
                    Matcher m = Pattern.compile("<DiagnosticID>(\\d+)</DiagnosticID>").matcher(body);
                    if (m.find()) {
                        return "✅ Diagnostic submitted. ID: " + m.group(1);
                    } else {
                        return "✅ Submitted but no ID found in response.";
                    }
                }
            } else {
                try (Scanner err = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                    String errorBody = err.hasNext() ? err.next() : "Unknown";
                    return "❌ Failed (" + responseCode + "): " + errorBody;
                }
            }
    
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }

    public String playSource(String ip, String uri, Integer volume, String deviceName) {
        try {
            // Set volume first
            ProcessBuilder volPb = new ProcessBuilder("SonosPlay", ip, "vol", String.valueOf(volume));
            Process volProc = volPb.start();
            int volExit = volProc.waitFor();
            if (volExit != 0) {
                return "❌ Failed to set volume for " + deviceName + " (" + ip + ")";
            }
            // Play source
            ProcessBuilder playPb = new ProcessBuilder("SonosPlay", ip, "play", uri);
            Process playProc = playPb.start();
            int playExit = playProc.waitFor();
            if (playExit == 0) {
                return "✅ Play command sent to " + deviceName + " (" + ip + ")";
            } else {
                return "❌ Failed to play source on " + deviceName + " (" + ip + ")";
            }
        } catch (Exception e) {
            return "❌ Exception for " + deviceName + " (" + ip + "): " + e.getMessage();
        }
    }

    // Set volume using SonosPlay CLI
    public String setVolume(String ip, Integer volume) {
        try {
            ProcessBuilder pb = new ProcessBuilder("SonosPlay", ip, "vol", String.valueOf(volume));
            Process proc = pb.start();
            int exit = proc.waitFor();
            if (exit == 0) {
                return "✅ Volume set to " + volume;
            } else {
                return "❌ Failed to set volume (exit " + exit + ")";
            }
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }

    // Get current volume percent using UPnP SOAP
    public String getVolume(String ip) {
        try {
            String soapEnvelope =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:GetVolume xmlns:u=\"urn:schemas-upnp-org:service:RenderingControl:1\">" +
                "<InstanceID>0</InstanceID>" +
                "<Channel>Master</Channel>" +
                "</u:GetVolume>" +
                "</s:Body>" +
                "</s:Envelope>";
            URL url = new URL("http://" + ip + ":1400/MediaRenderer/RenderingControl/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:RenderingControl:1#GetVolume\"");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200) {
                return "-";
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();
            String vol = xpath.evaluate("//*[local-name()='CurrentVolume']", doc);
            return vol != null && !vol.isEmpty() ? vol : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    // Pause playback using SonosPlay CLI
    public String pause(String ip) {
        try {
            ProcessBuilder pb = new ProcessBuilder("SonosPlay", ip, "pause");
            Process proc = pb.start();
            int exit = proc.waitFor();
            if (exit == 0) {
                return "⏸️ Paused";
            } else {
                return "❌ Failed to pause (exit " + exit + ")";
            }
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }

    // Resume playback using SonosPlay CLI
    public String resume(String ip) {
        try {
            ProcessBuilder pb = new ProcessBuilder("SonosPlay", ip, "play");
            Process proc = pb.start();
            int exit = proc.waitFor();
            if (exit == 0) {
                return "▶️ Resumed";
            } else {
                return "❌ Failed to resume (exit " + exit + ")";
            }
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }

    public String getPlaybackStatus(String ip) {
        try {
            String soapEnvelope =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:GetTransportInfo xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" +
                "<InstanceID>0</InstanceID>" +
                "</u:GetTransportInfo>" +
                "</s:Body>" +
                "</s:Envelope>";
            URL url = new URL("http://" + ip + ":1400/MediaRenderer/AVTransport/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#GetTransportInfo\"");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200) {
                return "❌ SOAP error: " + conn.getResponseCode();
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();
            String state = xpath.evaluate("//*[local-name()='CurrentTransportState']", doc);
            return state != null && !state.isEmpty() ? state : "❓ Unknown";
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }

    // Run SonosPlay CLI with arbitrary command (play, pause, etc)
    public String sonosCli(String ip, String cmd) {
        try {
            List<String> args = new ArrayList<>();
            args.add("SonosPlay");
            args.add(ip);
            // Split cmd into parts and add each as an argument
            for (String part : cmd.split("\\s+")) {
                args.add(part);
            }
            ProcessBuilder pb = new ProcessBuilder(args);
            Process proc = pb.start();
            int exit = proc.waitFor();
            String output = new String(proc.getInputStream().readAllBytes());
            String error = new String(proc.getErrorStream().readAllBytes());
            if (exit == 0) {
                return "✅ SonosPlay " + ip + " " + cmd + "\n" + output;
            } else {
                return "❌ SonosPlay failed (exit " + exit + ")\n" + error;
            }
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }

    /**
     * Get now playing info (title, artist, album, albumArtURI) from Sonos device via UPnP SOAP.
     */
    public Map<String, String> getNowPlayingInfo(String ip) {
        Map<String, String> result = new java.util.HashMap<>();
        try {
            String soapEnvelope =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:GetPositionInfo xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" +
                "<InstanceID>0</InstanceID>" +
                "</u:GetPositionInfo>" +
                "</s:Body>" +
                "</s:Envelope>";
            URL url = new URL("http://" + ip + ":1400/MediaRenderer/AVTransport/Control");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#GetPositionInfo\"");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200) {
                result.put("error", "SOAP error: " + conn.getResponseCode());
                return result;
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
            XPath xpath = XPathFactory.newInstance().newXPath();
            String trackMetaData = xpath.evaluate("//*[local-name()='TrackMetaData']", doc);
            if (trackMetaData == null || trackMetaData.isEmpty() || trackMetaData.equals("NOT_IMPLEMENTED")) {
                result.put("title", "");
                result.put("artist", "");
                result.put("album", "");
                result.put("albumArtURI", "");
                return result;
            }
            // Unescape XML
            String didl = StringEscapeUtils.unescapeXml(trackMetaData);
            // Fix unescaped ampersands (but not already escaped ones)
            didl = didl.replaceAll("&(?!amp;|lt;|gt;|apos;|quot;)", "&amp;");
            // Parse DIDL-Lite XML
            Document didlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(didl.getBytes(StandardCharsets.UTF_8)));
            String title = xpath.evaluate("//*[local-name()='title']", didlDoc);
            String artist = xpath.evaluate("//*[local-name()='creator']", didlDoc);
            String album = xpath.evaluate("//*[local-name()='album']", didlDoc);
            String albumArtURI = xpath.evaluate("//*[local-name()='albumArtURI']", didlDoc);
            // Some URIs may be relative, prepend device IP if needed
            if (albumArtURI != null && !albumArtURI.isEmpty() && albumArtURI.startsWith("/")) {
                albumArtURI = "http://" + ip + ":1400" + albumArtURI;
            }
            result.put("title", title != null ? title : "");
            result.put("artist", artist != null ? artist : "");
            result.put("album", album != null ? album : "");
            result.put("albumArtURI", albumArtURI != null ? albumArtURI : "");
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}
