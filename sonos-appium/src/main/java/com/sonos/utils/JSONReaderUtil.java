package com.sonos.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JSONReaderUtil {
    private static final Logger log = LogManager.getLogger(JSONReaderUtil.class);

    /**
     * Reads a JSON file and returns a list of data records
     * 
     * @param filePath Path to the JSON file
     * @return List of Map objects containing the JSON data
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> readJSON(String filePath) {
        List<Map<String, String>> records = new ArrayList<>();
        JSONParser parser = new JSONParser();
        
        try (FileReader reader = new FileReader(filePath)) {
            JSONArray jsonArray = (JSONArray) parser.parse(reader);
            
            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;
                records.add((Map<String, String>) jsonObject);
            }
            
            log.info("Successfully read " + records.size() + " records from JSON file: " + filePath);
        } catch (IOException | ParseException e) {
            log.error("Error reading JSON file: " + e.getMessage(), e);
        }
        
        return records;
    }
}
