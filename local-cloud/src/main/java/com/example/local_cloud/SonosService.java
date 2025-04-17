package com.example.local_cloud;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                Scanner errorScanner = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                String errorBody = errorScanner.hasNext() ? errorScanner.next() : "Unknown error";
                return "❌ Failed (" + responseCode + "):\n" + errorBody;
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

            Scanner scanner = new Scanner(getConn.getInputStream()).useDelimiter("\\A");
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
                Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String body = scanner.hasNext() ? scanner.next() : "";
                Matcher m = Pattern.compile("<DiagnosticID>(\\d+)</DiagnosticID>").matcher(body);
                if (m.find()) {
                    return "✅ Diagnostic submitted. ID: " + m.group(1);
                } else {
                    return "✅ Submitted but no ID found in response.";
                }
            } else {
                Scanner err = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                String errorBody = err.hasNext() ? err.next() : "Unknown";
                return "❌ Failed (" + responseCode + "): " + errorBody;
            }
    
        } catch (Exception e) {
            return "❌ Exception: " + e.getMessage();
        }
    }


    
    
}
