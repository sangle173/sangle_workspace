package com.example.selenium.tests;

import com.example.selenium.core.DriverManager;
import com.example.selenium.pages.MainPage;
import com.example.selenium.pages.SortableDataTablesPage;
import com.example.selenium.utils.TableUtils;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

public class SortableDataTablesTest {
    private WebDriver driver;
    private MainPage mainPage;
    private SortableDataTablesPage tablesPage;

    @BeforeClass
    public void setUp() {
        driver = DriverManager.getDriver();
        mainPage = new MainPage(driver);
        tablesPage = new SortableDataTablesPage(driver);
    }

    @Test
    public void testTableSorting() {
        mainPage.goTo();
        mainPage.clickSortableDataTables();
        List<List<String>> beforeSort = tablesPage.getTableData();
        tablesPage.sortByColumn(0); // Sort by first column
        List<List<String>> afterSort = tablesPage.getTableData();
        Assert.assertTrue(TableUtils.isColumnSorted(afterSort, 0), "Table is not sorted by first column");
    }

    @AfterClass
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
