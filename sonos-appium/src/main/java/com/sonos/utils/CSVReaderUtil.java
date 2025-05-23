package com.sonos.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CSVReaderUtil {
    public static List<String[]> readCSV(String filePath) {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                data.add(line.split(","));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV: " + e.getMessage());
        }
        return data;
    }
}
