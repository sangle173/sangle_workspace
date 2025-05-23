package com.sonos.data;

import org.testng.annotations.DataProvider;
import com.sonos.utils.CSVReaderUtil;
import java.util.List;

public class TestData {
    @DataProvider(name = "deviceData")
    public Object[][] deviceData() {
        List<String[]> records = CSVReaderUtil.readCSV("src/test/resources/test-data.csv");
        Object[][] data = new Object[records.size()][2];
        for (int i = 0; i < records.size(); i++) {
            data[i] = records.get(i);
        }
        return data;
    }
}
