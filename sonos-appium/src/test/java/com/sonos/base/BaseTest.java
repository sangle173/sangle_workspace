package com.sonos.base;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import com.sonos.utils.DriverManager;

public class BaseTest {
    @BeforeClass
    public void setUp() {
        DriverManager.initDriver();
    }

    @AfterClass
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
