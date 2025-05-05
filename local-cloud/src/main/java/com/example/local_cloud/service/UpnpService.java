package com.example.local_cloud.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
public class UpnpService {
    
    private static final String UPNP_DEVICE_TYPE = "urn:schemas-upnp-org:device:ZonePlayer:1";
    private static final String UPNP_SERVICE_TYPE = "urn:schemas-upnp-org:service:ZoneGroupTopology:1";
    
    public Map<String, String> getDeviceDescription(String location) throws Exception {
        URL url = new URL(location);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(conn.getInputStream());
        
        Map<String, String> deviceInfo = new HashMap<>();
        
        // Extract device information
        deviceInfo.put("friendlyName", getElementValue(doc, "friendlyName"));
        deviceInfo.put("manufacturer", getElementValue(doc, "manufacturer"));
        deviceInfo.put("modelName", getElementValue(doc, "modelName"));
        deviceInfo.put("modelNumber", getElementValue(doc, "modelNumber"));
        deviceInfo.put("serialNumber", getElementValue(doc, "serialNumber"));
        deviceInfo.put("UDN", getElementValue(doc, "UDN"));
        
        // Extract service information
        NodeList serviceList = doc.getElementsByTagName("service");
        for (int i = 0; i < serviceList.getLength(); i++) {
            Element service = (Element) serviceList.item(i);
            String serviceType = service.getElementsByTagName("serviceType").item(0).getTextContent();
            String controlURL = service.getElementsByTagName("controlURL").item(0).getTextContent();
            deviceInfo.put(serviceType, controlURL);
        }
        
        return deviceInfo;
    }
    
    public String sendUpnpAction(String ip, String serviceType, String actionName, Map<String, String> parameters) throws Exception {
        String controlURL = getControlURL(ip, serviceType);
        if (controlURL == null) {
            throw new Exception("Service not found: " + serviceType);
        }
        
        StringBuilder soapBody = new StringBuilder();
        soapBody.append("<u:").append(actionName).append(" xmlns:u=\"").append(serviceType).append("\">");
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            soapBody.append("<").append(param.getKey()).append(">")
                    .append(param.getValue())
                    .append("</").append(param.getKey()).append(">");
        }
        soapBody.append("</u:").append(actionName).append(">");
        
        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" + soapBody + "</s:Body>" +
                "</s:Envelope>";
        
        URL url = new URL("http://" + ip + ":1400" + controlURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        conn.setRequestProperty("SOAPACTION", "\"" + serviceType + "#" + actionName + "\"");
        
        conn.getOutputStream().write(soapEnvelope.getBytes());
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("UPnP action failed: " + conn.getResponseCode());
        }
        
        return parseResponse(conn);
    }
    
    private String getControlURL(String ip, String serviceType) throws Exception {
        // This would typically be cached from device description
        // For now, we'll return the standard control URL
        return "/" + serviceType.split(":")[3] + "/Control";
    }
    
    private String parseResponse(HttpURLConnection conn) throws Exception {
        // Read the raw response first
        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine()).append("\n");
            }
        }
        
        String rawResponse = response.toString();
        System.out.println("Raw UPnP response:\n" + rawResponse);
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(rawResponse)));
            
            // First try SOAP format
            NodeList bodyNodes = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
            if (bodyNodes.getLength() > 0) {
                Element body = (Element) bodyNodes.item(0);
                NodeList responseNodes = body.getElementsByTagNameNS("*", "Response");
                if (responseNodes.getLength() > 0) {
                    Element responseElement = (Element) responseNodes.item(0);
                    return responseElement.getTextContent();
                }
            }
            
            // If not SOAP, try direct XML response
            NodeList responseNodes = doc.getElementsByTagNameNS("*", "Response");
            if (responseNodes.getLength() > 0) {
                Element responseElement = (Element) responseNodes.item(0);
                return responseElement.getTextContent();
            }
            
            // If no Response element found, return the entire document content
            return doc.getDocumentElement().getTextContent();
            
        } catch (Exception e) {
            System.out.println("Error parsing XML response: " + e.getMessage());
            // If XML parsing fails, return the raw response
            return rawResponse;
        }
    }
    
    private String getElementValue(Document doc, String tagName) {
        NodeList elements = doc.getElementsByTagName(tagName);
        return elements.getLength() > 0 ? elements.item(0).getTextContent() : null;
    }
} 