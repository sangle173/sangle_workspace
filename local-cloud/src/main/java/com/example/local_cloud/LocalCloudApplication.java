package com.example.local_cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class LocalCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocalCloudApplication.class, args);
	}
}

@Component
class StartupInfoLogger {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationStartup() {
        System.out.println("\n----------------------------------------------------------");
        System.out.println("   Local Cloud Application is running! Access URLs:");
        System.out.println("   Local:      http://localhost:" + serverPort);
        
        List<String> addresses = getNetworkAddresses();
        if (!addresses.isEmpty()) {
            System.out.println("   Network Addresses:");
            for (String address : addresses) {
                System.out.println("      → http://" + address + ":" + serverPort);
            }
        }
        System.out.println("----------------------------------------------------------\n");
    }
    
    private List<String> getNetworkAddresses() {
        List<String> addresses = new ArrayList<>();
        List<String> ethernetAddresses = new ArrayList<>();
        List<String> wifiAddresses = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // Skip loopback, virtual, and non-running interfaces
                if (networkInterface.isLoopback() || 
                    !networkInterface.isUp() || 
                    networkInterface.isVirtual()) {
                    continue;
                }
                
                String interfaceName = networkInterface.getName().toLowerCase();
                
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    if (address.getHostAddress().contains(":")) {
                        // Skip IPv6 addresses for simplicity
                        continue;
                    }
                    
                    if (interfaceName.startsWith("eth") || interfaceName.startsWith("en")) {
                        // Ethernet
                        ethernetAddresses.add(address.getHostAddress());
                    } else if (interfaceName.startsWith("wl")) {
                        // WiFi
                        wifiAddresses.add(address.getHostAddress());
                    } else {
                        // Other interfaces
                        addresses.add(address.getHostAddress());
                    }
                }
            }
            
            // Prioritize Ethernet over WiFi, but include both
            if (!ethernetAddresses.isEmpty()) {
                addresses.addAll(ethernetAddresses);
            }
            
            if (!wifiAddresses.isEmpty()) {
                addresses.addAll(wifiAddresses);
            }
        } catch (SocketException e) {
            System.err.println("Failed to get network interfaces: " + e.getMessage());
        }
        
        return addresses;
    }
}
