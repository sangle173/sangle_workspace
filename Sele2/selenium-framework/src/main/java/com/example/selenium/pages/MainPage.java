package com.example.selenium.pages;

import com.example.selenium.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MainPage extends BasePage {
    private By sortableDataTablesLink = By.linkText("Sortable Data Tables");

    public MainPage(WebDriver driver) {
        super(driver);
    }

    public void goTo() {
        driver.get("https://the-internet.herokuapp.com/");
    }

    public void clickSortableDataTables() {
        driver.findElement(sortableDataTablesLink).click();
    }
}
