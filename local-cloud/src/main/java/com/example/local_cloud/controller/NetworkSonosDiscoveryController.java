package com.example.local_cloud.controller;

import com.example.local_cloud.service.NetworkSonosDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
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
}
