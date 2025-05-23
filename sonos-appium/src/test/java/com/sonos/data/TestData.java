package com.sonos.data;

import org.testng.annotations.DataProvider;
import com.sonos.utils.JSONReaderUtil;
import java.util.List;
import java.util.Map;

public class TestData {
    @DataProvider(name = "deviceData")
    public Object[][] deviceData() {
        List<Map<String, String>> records = JSONReaderUtil.readJSON("src/test/resources/test-data.json");
        Object[][] data = new Object[records.size()][2]; // We need deviceName and macAddress
        
        for (int i = 0; i < records.size(); i++) {
            Map<String, String> record = records.get(i);
            data[i][0] = record.get("deviceName");
            data[i][1] = record.get("macAddress");
        }
        
        return data;
    }
}
