package com.sonos.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

package com.sonos.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {
    private int count = 0;
    private static final int MAX_RETRIES = 2;

    @Override
    public boolean retry(ITestResult result) {
        return count++ < MAX_RETRIES;
    }
}
