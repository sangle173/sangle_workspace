package com.example.local_cloud.controller;

import com.example.local_cloud.service.NetworkSonosDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/network-sonos-discovery")
public class NetworkSonosDiscoveryController {
    @Autowired
    private NetworkSonosDiscoveryService discoveryService;

    @GetMapping
    public String view(Model model) {
        return "network-sonos-discovery";
    }

    @GetMapping("/wifi-list")
    @ResponseBody
    public List<Map<String, String>> getWifiList() {
        return discoveryService.getAvailableWifiNetworks();
    }

    @PostMapping("/scan")
    @ResponseBody
    public Map<String, Object> scanAllNetworks() {
        return discoveryService.scanAllNetworks();
    }

    @PostMapping("/auto-scan")
    @ResponseBody
    public Map<String, Object> autoScanAllWifiNetworks() {
        return discoveryService.autoScanAllWifiNetworks();
    }

    @GetMapping("/devices")
    public String showDevices(Model model) {
        model.addAttribute("devicesByNetwork", discoveryService.getDevicesByNetwork());
        return "network-sonos-discovery-devices";
    }

    @GetMapping("/auto-scan/progress")
    public SseEmitter autoScanAllWifiNetworksProgress() {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        new Thread(() -> {
            try {
                discoveryService.autoScanAllWifiNetworksWithProgress(msg -> {
                    try {
                        emitter.send(SseEmitter.event().data(msg));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.send(SseEmitter.event().data("__SCAN_DONE__"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    @GetMapping("/all-devices-json")
    @ResponseBody
    public List<Map<String, String>> getAllDevicesJson() {
        List<Map<String, String>> all = discoveryService.readDevicesFromJson();
        return all;
    }
}
