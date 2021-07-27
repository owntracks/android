package org.owntracks.android.ui

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.preferences.PreferencesActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferencesActivityTests :
    TestWithAnActivity<PreferencesActivity>(PreferencesActivity::class.java) {
    @Test
    @AllowFlaky(attempts = 1)
    fun initialViewShowsTopLevelMenu() {
        assertDisplayed(R.string.preferencesServer)
        assertDisplayed(R.string.preferencesReporting)
        assertDisplayed(R.string.preferencesNotification)
        assertDisplayed(R.string.preferencesAdvanced)
        assertDisplayed(R.string.configurationManagement)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun httpURLBlankIsInvalidURL() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        writeToEditTextDialog(R.string.preferencesUrl, "")
        assertContainsError(
            android.R.id.edit,
            baristaRule.activityTestRule.activity.getString(R.string.preferencesUrlValidationError)
        )
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun httpURLSimpleStringIsInvalidURL() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        writeToEditTextDialog(R.string.preferencesUrl, "testText")
        assertContainsError(
            android.R.id.edit,
            baristaRule.activityTestRule.activity.getString(R.string.preferencesUrlValidationError)
        )
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun mqttKeepaliveBelowMinimumIsInvalid() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_mqtt_private_label)
        writeToEditTextDialog(R.string.preferencesKeepalive, "899")
        assertContainsError(android.R.id.edit, "Keepalive should be a minimum")
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun mqttKeepaliveAtMinimumIsValid() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_mqtt_private_label)
        writeToEditTextDialog(R.string.preferencesKeepalive, "900")
        assertNotExist(android.R.id.edit)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun settingSimpleMQTTConfigSettingsCanBeShownInEditor() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_mqtt_private_label)

        writeToEditTextDialog(R.string.preferencesHost, "mqtt.example.com")
        writeToEditTextDialog(R.string.preferencesPort, "1234")
        writeToEditTextDialog(R.string.preferencesClientId, "test-clientId")

        clickOnAndWait(R.string.preferencesWebsocket)
        writeToEditTextDialog(R.string.preferencesUserUsername, "testUsername")
        writeToEditTextDialog(R.string.preferencesBrokerPassword, "testPassword")
        writeToEditTextDialog(R.string.preferencesDeviceName, "testDeviceId")
        writeToEditTextDialog(R.string.preferencesTrackerId, "t5")

        scrollToText(R.string.preferencesCleanSessionEnabled)

        clickOnAndWait(R.string.preferencesCleanSessionEnabled)
        writeToEditTextDialog(R.string.preferencesKeepalive, "1570")

        clickBackAndWait()
        clickOnAndWait(R.string.configurationManagement)

        assertContains(R.id.effectiveConfiguration, "\"_type\" : \"configuration\"")
        assertContains(R.id.effectiveConfiguration, " \"waypoints\" : [ ]")

        assertContains(R.id.effectiveConfiguration, "\"host\" : \"mqtt.example.com\"")
        assertContains(R.id.effectiveConfiguration, "\"port\" : 1234")
        assertContains(R.id.effectiveConfiguration, "\"clientId\" : \"test-clientId\"")
        assertContains(R.id.effectiveConfiguration, "\"username\" : \"testUsername\"")
        assertContains(R.id.effectiveConfiguration, "\"password\" : \"********\"")
        assertContains(R.id.effectiveConfiguration, "\"deviceId\" : \"testDeviceId\"")
        assertContains(R.id.effectiveConfiguration, "\"tid\" : \"t5\"")

        assertContains(R.id.effectiveConfiguration, "\"pubQos\" : 1")
        assertContains(R.id.effectiveConfiguration, "\"subQos\" : 2")
        assertContains(R.id.effectiveConfiguration, "\"info\" : true")
        assertContains(R.id.effectiveConfiguration, "\"tlsCaCrt\" : \"\"")
        assertContains(R.id.effectiveConfiguration, "\"tlsClientCrt\" : \"\"")
        assertContains(R.id.effectiveConfiguration, "\"tlsClientCrtPassword\" : \"\"")
        assertContains(R.id.effectiveConfiguration, "\"tls\" : true")
        assertContains(R.id.effectiveConfiguration, "\"mqttProtocolLevel\" : 4")
        assertContains(R.id.effectiveConfiguration, "\"subTopic\" : \"owntracks/+/+\"")
        assertContains(R.id.effectiveConfiguration, "\"pubTopicBase\" : \"owntracks/%u/%d\"")
        assertContains(R.id.effectiveConfiguration, "\"ws\" : true")

        assertNotContains(R.id.effectiveConfiguration, "\"url\"")
        assertNotContains(R.id.effectiveConfiguration, "\"preferenceKeyDontReuseHttpClient\"")
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun settingSimpleHTTPConfigSettingsCanBeShownInEditor() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        writeToEditTextDialog(R.string.preferencesUrl, "https://www.example.com:8080/")
        writeToEditTextDialog(R.string.preferencesUserUsername, "testUsername")
        writeToEditTextDialog(R.string.preferencesBrokerPassword, "testPassword")
        writeToEditTextDialog(R.string.preferencesDeviceName, "testDeviceId")
        writeToEditTextDialog(R.string.preferencesTrackerId, "t1")
        clickBackAndWait()


        clickOnAndWait(R.string.preferencesReporting)
        clickOnAndWait(R.string.preferencesPubExtendedData)
        clickBackAndWait()

        clickOnAndWait(R.string.preferencesNotification)
        clickOnAndWait(R.string.preferencesNotificationEvents)
        clickBackAndWait()

        clickOnAndWait(R.string.preferencesAdvanced)
        clickOnAndWait(R.string.preferencesRemoteCommand)

        writeToEditTextDialog(R.string.preferencesIgnoreInaccurateLocations, "950")
        writeToEditTextDialog(R.string.preferencesLocatorInterval, "123")
        writeToEditTextDialog(R.string.preferencesMoveModeLocatorInterval, "5")

        scrollToText(R.string.preferencesAutostart)
        clickOnAndWait(R.string.preferencesAutostart)

        scrollToText(R.string.preferencesReverseGeocodeProvider)
        clickOnAndWait(R.string.preferencesReverseGeocodeProvider)

        clickOnAndWait("OpenCage")

        writeToEditTextDialog(R.string.preferencesOpencageGeocoderApiKey, "geocodeAPIKey")

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
        scrollToText(R.string.preferencesReverseGeocodeProvider)
        assertDisplayed(R.string.valDefaultGeocoder)
    }

    private fun scrollToText(textResource: Int) {
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(textResource)), scrollTo()
                )
            )
    }

    private fun writeToEditTextDialog(@StringRes name: Int, value: String) {
        scrollToText(name)
        clickOnAndWait(name)
        writeTo(android.R.id.edit, value)
        clickDialogPositiveButton()
    }
}