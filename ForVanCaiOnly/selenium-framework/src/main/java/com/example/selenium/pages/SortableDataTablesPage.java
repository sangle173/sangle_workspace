package com.example.selenium.pages;

import com.example.selenium.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;

public class SortableDataTablesPage extends BasePage {
    private By tableRows = By.cssSelector("#table1 tbody tr");
    private By tableHeaders = By.cssSelector("#table1 thead th");

    public SortableDataTablesPage(WebDriver driver) {
        super(driver);
    }

    public List<List<String>> getTableData() {
        List<List<String>> data = new ArrayList<>();
        List<WebElement> rows = driver.findElements(tableRows);
        for (WebElement row : rows) {
            List<String> rowData = new ArrayList<>();
            for (WebElement cell : row.findElements(By.tagName("td"))) {
                rowData.add(cell.getText());
            }
            data.add(rowData);
        }
        return data;
    }

    public void sortByColumn(int columnIndex) {
        List<WebElement> headers = driver.findElements(tableHeaders);
        headers.get(columnIndex).click();
    }
}
