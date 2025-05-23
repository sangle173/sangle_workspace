package com.sonos.tests;

import com.sonos.base.BaseTest;
import com.sonos.pages.WelcomePage;
import com.sonos.pages.BeamSetupPage;
import com.sonos.data.TestData;
import com.sonos.utils.RetryAnalyzer;
import org.testng.annotations.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

package com.sonos.tests;

import com.sonos.base.BaseTest;
import com.sonos.pages.WelcomePage;
import com.sonos.pages.BeamSetupPage;
import com.sonos.data.TestData;
import com.sonos.utils.RetryAnalyzer;
import org.testng.annotations.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BeamSetupTest extends BaseTest {
    private static final Logger log = LogManager.getLogger(BeamSetupTest.class);

    @Test(dataProvider = "deviceData", dataProviderClass = TestData.class, retryAnalyzer = RetryAnalyzer.class)
    public void testAddSonosBeamWithNewHousehold(String deviceName, String macAddress) {
        log.info("Starting setup for: " + deviceName);
        new WelcomePage().clickNextButton();
        BeamSetupPage beam = new BeamSetupPage();
        beam.tapAddProduct();
        beam.selectBeamByName(deviceName);
        beam.tapSetupProduct();
        beam.tapCreateNewHousehold();
        beam.selectDeviceByMac(macAddress);
        beam.completeSetup();
        log.info("Setup complete for: " + deviceName);
    }
}
