package org.owntracks.android.ui

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.e2e.scrollToPreferenceWithText
import org.owntracks.android.ui.preferences.PreferencesActivity


@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferencesActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(PreferencesActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }

    @Before
    fun initIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    @AllowFlaky
    fun initialViewShowsTopLevelMenu() {
        assertDisplayed(R.string.preferencesServer)
        assertDisplayed(R.string.preferencesReporting)
        assertDisplayed(R.string.preferencesNotification)
        assertDisplayed(R.string.preferencesAdvanced)
        assertDisplayed(R.string.configurationManagement)
    }


    @Test
    @AllowFlaky(attempts = 3)
    fun settingSimpleHTTPConfigSettingsCanBeShownInEditor() {

        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        clickDialogPositiveButton()
        clickOnAndWait(R.string.preferencesHost)
        writeTo(R.id.url, "https://www.example.com:8080/")
        clickDialogPositiveButton()
        clickOnAndWait(R.string.preferencesIdentification)
        writeTo(R.id.username, "testUsername")
        writeTo(R.id.password, "testPassword")
        writeTo(R.id.deviceId, "testDeviceId")
        writeTo(R.id.trackerId, "t1")
        clickDialogPositiveButton()
        clickBackAndWait()


        clickOnAndWait(R.string.preferencesReporting)
        clickOnAndWait(R.string.preferencesPubExtendedData)
        clickBackAndWait()

        clickOnAndWait(R.string.preferencesNotification)
        clickOnAndWait(R.string.preferencesNotificationEvents)
        clickBackAndWait()

        // This is an ugly hack, but there's some race conditions on underpowered hardware
        // causing the test to move on before the view has been fully built/rendered.

        clickOnAndWait(R.string.preferencesAdvanced)
        clickOnAndWait(R.string.preferencesRemoteCommand)
        clickOnAndWait(R.string.preferencesIgnoreInaccurateLocations)

        writeTo(android.R.id.edit, "950")

        clickDialogPositiveButton()

        clickOnAndWait(R.string.preferencesLocatorInterval)

        writeTo(android.R.id.edit, "123")

        clickDialogPositiveButton()

        scrollToPreferenceWithText(R.string.preferencesMoveModeLocatorInterval)
        clickOnAndWait(R.string.preferencesMoveModeLocatorInterval)

        writeTo(android.R.id.edit, "5")

        clickDialogPositiveButton()

        scrollToPreferenceWithText(R.string.preferencesAutostart)
        clickOnAndWait(R.string.preferencesAutostart)

        scrollToPreferenceWithText(R.string.preferencesReverseGeocodeProvider)
        clickOnAndWait(R.string.preferencesReverseGeocodeProvider)

        clickOnAndWait("OpenCage")

        scrollToPreferenceWithText(R.string.preferencesOpencageGeocoderApiKey)
        clickOnAndWait(R.string.preferencesOpencageGeocoderApiKey)
        writeTo(android.R.id.edit, "geocodeAPIKey")
        clickDialogPositiveButton()

        clickBackAndWait()

        clickOnAndWait(R.string.configurationManagement)

        assertContains(R.id.effectiveConfiguration, "\"_type\" : \"configuration\"")
        assertContains(R.id.effectiveConfiguration, " \"waypoints\" : [ ]")

        assertContains(R.id.effectiveConfiguration, "\"url\" : \"https://www.example.com:8080/\"")
        assertContains(R.id.effectiveConfiguration, "\"username\" : \"testUsername\"")
        assertContains(R.id.effectiveConfiguration, "\"password\" : \"********\"")
        assertContains(R.id.effectiveConfiguration, "\"deviceId\" : \"testDeviceId\"")
        assertContains(R.id.effectiveConfiguration, "\"tid\" : \"t1\"")
        assertContains(R.id.effectiveConfiguration, "\"notificationEvents\" : false")
        assertContains(R.id.effectiveConfiguration, "\"pubExtendedData\" : false")
        assertContains(R.id.effectiveConfiguration, "\"ignoreInaccurateLocations\" : 950")
        assertContains(R.id.effectiveConfiguration, "\"locatorInterval\" : 123")
        assertContains(R.id.effectiveConfiguration, "\"moveModeLocatorInterval\" : 5")
        assertContains(R.id.effectiveConfiguration, "\"autostartOnBoot\" : false")
        assertContains(R.id.effectiveConfiguration, "\"reverseGeocodeProvider\" : \"OpenCage\"")
        assertContains(R.id.effectiveConfiguration, "\"opencageApiKey\" : \"geocodeAPIKey\"")
        assertContains(R.id.effectiveConfiguration, "\"connectionTimeoutSeconds\" : 30")

        // Make sure that the MQTT-specific settings aren't present
        assertNotContains(R.id.effectiveConfiguration, "\"host\"")
        assertNotContains(R.id.effectiveConfiguration, "\"port\"")
        assertNotContains(R.id.effectiveConfiguration, "\"pubQos\"")
        assertNotContains(R.id.effectiveConfiguration, "\"subQos\"")
        assertNotContains(R.id.effectiveConfiguration, "\"info\"")
        assertNotContains(R.id.effectiveConfiguration, "\"tlsCaCrt\"")
        assertNotContains(R.id.effectiveConfiguration, "\"tls\"")
        assertNotContains(R.id.effectiveConfiguration, "\"mqttProtocolLevel\"")
        assertNotContains(R.id.effectiveConfiguration, "\"subTopic\"")
        assertNotContains(R.id.effectiveConfiguration, "\"pubTopicBase\"")
        assertNotContains(R.id.effectiveConfiguration, "\"clientId\"")
    }

    @Test
    @AllowFlaky
    fun defaultGeocoderIsSelected() {
        clickOnAndWait(R.string.preferencesAdvanced)
        scrollToPreferenceWithText(R.string.preferencesReverseGeocodeProvider)
        assertDisplayed(baristaRule.activityTestRule.activity.resources.getStringArray(R.array.geocoders)[1])
    }
}