package com.example.selenium.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableUtils {
    public static boolean isColumnSorted(List<List<String>> tableData, int columnIndex) {
        List<String> column = new ArrayList<>();
        for (List<String> row : tableData) {
            if (row.size() > columnIndex) {
                column.add(row.get(columnIndex));
            }
        }
        List<String> sorted = new ArrayList<>(column);
        Collections.sort(sorted);
        return column.equals(sorted);
    }
}
