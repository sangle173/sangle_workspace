package com.example.local_cloud.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.*;
import java.util.*;

public class SonosUpdateWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Map<String, Object> req = objectMapper.readValue(
            message.getPayload(),
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
        );
        List<String> ips = null;
        // Support both old (devices) and new (hhid) protocol
        if (req.containsKey("hhid")) {
            String hhid = (String) req.get("hhid");
            ips = com.example.local_cloud.controller.SonosActionController.getDeviceIpsForHHID(hhid);
        } else {
            Object devObj = req.get("devices");
            if (devObj instanceof List<?>) {
                ips = new ArrayList<>();
                for (Object o : (List<?>) devObj) {
                    if (o != null) ips.add(o.toString());
                }
            }
        }
        String updateLink = (String) req.get("updateLink");
        if (ips == null || ips.isEmpty()) {
            session.sendMessage(new TextMessage("❌ No devices found for this HHID."));
            return;
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("stdbuf");
        cmd.add("-oL");
        cmd.add("SonosUpdate");
        for (String ip : ips) {
            cmd.add("--ip");
            cmd.add(ip);
        }
        cmd.add("--uri");
        cmd.add(updateLink);
        session.sendMessage(new TextMessage("[SonosUpdate] Executed: " + String.join(" ", cmd)));
        // Run process in a new thread for real-time streaming
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        session.sendMessage(new TextMessage(line));
                        System.out.println("[SonosUpdate] " + line); // Log to Spring Boot console
                    }
                }
                proc.waitFor();
                session.sendMessage(new TextMessage("✅ SonosUpdate completed."));
                System.out.println("[SonosUpdate] ✅ SonosUpdate completed."); // Log completion
            } catch (Exception e) {
                try { session.sendMessage(new TextMessage("❌ Error: " + e)); } catch (Exception ignore) {}
                System.err.println("[SonosUpdate] ❌ Error: " + e); // Log error
            }
        }).start();
    }
}