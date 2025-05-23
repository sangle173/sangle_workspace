package com.sonos.pages;

import io.appium.java_client.AppiumBy;
import com.sonos.utils.DriverManager;

public class WelcomePage {
    public void clickNextButton() {
        DriverManager.driver.findElement(AppiumBy.accessibilityId("Next")).click();
    }
}
