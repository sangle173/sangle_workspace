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
        Map<String, Object> req = objectMapper.readValue(message.getPayload(), Map.class);
        List<String> ips = (List<String>) req.get("devices");
        String updateLink = (String) req.get("updateLink");

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
                    }
                }
                int exit = proc.waitFor();
                if (exit == 0) {
                    session.sendMessage(new TextMessage("✅ SonosUpdate completed."));
                } else {
                    session.sendMessage(new TextMessage("❌ SonosUpdate failed (exit " + exit + ")"));
                }
            } catch (Exception e) {
                try { session.sendMessage(new TextMessage("❌ Exception: " + e.getMessage())); } catch (IOException ignored) {}
            } finally {
                try { session.close(); } catch (IOException ignored) {}
            }
        }).start();
    }
} 