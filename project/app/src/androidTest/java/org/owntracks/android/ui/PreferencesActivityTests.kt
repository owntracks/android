package org.owntracks.android.ui

import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.preferences.DefaultsProviderImpl
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.SharedPreferencesStore
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.scrollToPreferenceWithText
import org.owntracks.android.testutils.writeToPreference
import org.owntracks.android.ui.preferences.PreferencesActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferencesActivityTests : TestWithAnActivity<PreferencesActivity>(PreferencesActivity::class.java) {
    @Test
    fun initialViewShowsTopLevelMenu() {
        assertDisplayed(R.string.preferencesServer)
        assertDisplayed(R.string.preferencesReporting)
        assertDisplayed(R.string.preferencesNotification)
        assertDisplayed(R.string.preferencesAdvanced)
        assertDisplayed(R.string.configurationManagement)
    }

    @Test
    fun settingSimpleMQTTConfigSettingsCanBeShownInEditor() {
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_mqtt_private_label)

        writeToPreference(R.string.preferencesHost, "mqtt.example.com")
        writeToPreference(R.string.preferencesPort, "1234")
        writeToPreference(R.string.preferencesClientId, "test-clientId") // This hyphen will get squelched

        scrollToPreferenceWithText(R.string.preferencesWebsocket)
        clickOnAndWait(R.string.preferencesWebsocket)

        writeToPreference(R.string.preferencesUsername, "testUsername")
        writeToPreference(R.string.preferencesBrokerPassword, "testPassword")
        writeToPreference(R.string.preferencesDeviceName, "testDeviceId")
        writeToPreference(R.string.preferencesTrackerId, "t5")

        scrollToPreferenceWithText(R.string.preferencesCleanSessionEnabled)
        clickOnAndWait(R.string.preferencesCleanSessionEnabled)

        writeToPreference(R.string.preferencesKeepalive, "1570")

        clickBackAndWait()
        clickOnAndWait(R.string.configurationManagement)

        assertContains(R.id.effectiveConfiguration, "\"_type\" : \"configuration\"")
        assertContains(R.id.effectiveConfiguration, " \"waypoints\" : [ ]")

        assertContains(R.id.effectiveConfiguration, "\"host\" : \"mqtt.example.com\"")
        assertContains(R.id.effectiveConfiguration, "\"port\" : 1234")
        assertContains(R.id.effectiveConfiguration, "\"clientId\" : \"testclientId\"")
        assertContains(R.id.effectiveConfiguration, "\"username\" : \"testUsername\"")
        assertContains(R.id.effectiveConfiguration, "\"password\" : \"testPassword\"")
        assertContains(R.id.effectiveConfiguration, "\"deviceId\" : \"testDeviceId\"")
        assertContains(R.id.effectiveConfiguration, "\"tid\" : \"t5\"")

        assertContains(R.id.effectiveConfiguration, "\"pubQos\" : 1")
        assertContains(R.id.effectiveConfiguration, "\"subQos\" : 2")
        assertContains(R.id.effectiveConfiguration, "\"info\" : true")
        assertContains(R.id.effectiveConfiguration, "\"tlsClientCrt\" : \"\"")
        assertContains(R.id.effectiveConfiguration, "\"tlsClientCrtPassword\" : \"\"")
        assertContains(R.id.effectiveConfiguration, "\"tls\" : true")
        assertContains(R.id.effectiveConfiguration, "\"mqttProtocolLevel\" : 3")
        assertContains(R.id.effectiveConfiguration, "\"subTopic\" : \"owntracks/+/+\"")
        assertContains(R.id.effectiveConfiguration, "\"pubTopicBase\" : \"owntracks/%u/%d\"")
        assertContains(R.id.effectiveConfiguration, "\"ws\" : true")

        assertNotContains(R.id.effectiveConfiguration, "\"url\"")
        assertNotContains(R.id.effectiveConfiguration, "\"preferenceKeyDontReuseHttpClient\"")
    }

    @Test
    fun settingSimpleHTTPConfigSettingsCanBeShownInEditor() {
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit()
            .putBoolean(Preferences::notificationEvents.name, false)
            .apply()
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)

        writeToPreference(R.string.preferencesUrl, "https://www.example.com:8080/")

        writeToPreference(R.string.preferencesUsername, "testUsername")
        writeToPreference(R.string.preferencesBrokerPassword, "testPassword")
        writeToPreference(R.string.preferencesDeviceName, "testDeviceId")
        writeToPreference(R.string.preferencesTrackerId, "t1")
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
        writeToPreference(R.string.preferencesIgnoreInaccurateLocations, "950")
        writeToPreference(R.string.preferencesLocatorInterval, "123")
        writeToPreference(R.string.preferencesLocatorDisplacement, "567")
        writeToPreference(R.string.preferencesMoveModeLocatorInterval, "5")

        scrollToPreferenceWithText(R.string.preferencesPegLocatorFastestIntervalToInterval)
        clickOnAndWait(R.string.preferencesPegLocatorFastestIntervalToInterval)

        scrollToPreferenceWithText(R.string.preferencesAutostart)
        clickOnAndWait(R.string.preferencesAutostart)

        scrollToPreferenceWithText(R.string.preferencesReverseGeocodeProvider)
        clickOnAndWait(R.string.preferencesReverseGeocodeProvider)

        clickOnAndWait("OpenCage")

        writeToPreference(R.string.preferencesOpencageGeocoderApiKey, "geocodeAPIKey")

        clickBackAndWait()

        clickOnAndWait(R.string.configurationManagement)

        assertContains(R.id.effectiveConfiguration, "\"_type\" : \"configuration\"")
        assertContains(R.id.effectiveConfiguration, " \"waypoints\" : [ ]")

        assertContains(R.id.effectiveConfiguration, "\"url\" : \"https://www.example.com:8080/\"")
        assertContains(R.id.effectiveConfiguration, "\"username\" : \"testUsername\"")
        assertContains(R.id.effectiveConfiguration, "\"password\" : \"testPassword\"")
        assertContains(R.id.effectiveConfiguration, "\"deviceId\" : \"testDeviceId\"")
        assertContains(R.id.effectiveConfiguration, "\"tid\" : \"t1\"")
        assertContains(R.id.effectiveConfiguration, "\"notificationEvents\" : true")
        assertContains(R.id.effectiveConfiguration, "\"pubExtendedData\" : false")
        assertContains(R.id.effectiveConfiguration, "\"ignoreInaccurateLocations\" : 950")
        assertContains(R.id.effectiveConfiguration, "\"locatorInterval\" : 123")
        assertContains(R.id.effectiveConfiguration, "\"locatorDisplacement\" : 567")
        assertContains(R.id.effectiveConfiguration, "\"moveModeLocatorInterval\" : 5")
        assertContains(R.id.effectiveConfiguration, "\"autostartOnBoot\" : false")
        assertContains(R.id.effectiveConfiguration, "\"reverseGeocodeProvider\" : \"OpenCage\"")
        assertContains(R.id.effectiveConfiguration, "\"opencageApiKey\" : \"geocodeAPIKey\"")
        assertContains(R.id.effectiveConfiguration, "\"connectionTimeoutSeconds\" : 30")
        assertContains(R.id.effectiveConfiguration, "\"pegLocatorFastestIntervalToInterval\" : true")

        // Make sure that the MQTT-specific settings aren't present
        assertNotContains(R.id.effectiveConfiguration, "\"host\"")
        assertNotContains(R.id.effectiveConfiguration, "\"port\"")
        assertNotContains(R.id.effectiveConfiguration, "\"pubQos\"")
        assertNotContains(R.id.effectiveConfiguration, "\"subQos\"")
        assertNotContains(R.id.effectiveConfiguration, "\"info\"")
        assertNotContains(R.id.effectiveConfiguration, "\"tls\"")
        assertNotContains(R.id.effectiveConfiguration, "\"mqttProtocolLevel\"")
        assertNotContains(R.id.effectiveConfiguration, "\"subTopic\"")
        assertNotContains(R.id.effectiveConfiguration, "\"pubTopicBase\"")
        assertNotContains(R.id.effectiveConfiguration, "\"clientId\"")
    }

    @Test
    fun defaultGeocoderIsSelected() {
        clickOnAndWait(R.string.preferencesAdvanced)
        scrollToPreferenceWithText(R.string.preferencesReverseGeocodeProvider)
        val defaultGeocoder = baristaRule.activityTestRule.activity.applicationContext.let {
            DefaultsProviderImpl().getDefaultValue<ReverseGeocodeProvider>(
                Preferences(
                    SharedPreferencesStore(it, NotificationManagerCompat.from(app), NotificationsStash()),
                    SimpleIdlingResource("unused", true)
                ),
                Preferences::reverseGeocodeProvider
            )
        }
        val expected = baristaRule.activityTestRule.activity.resources.run {
            getStringArray(R.array.geocoders)[
                getStringArray(R.array.geocoderValues).indexOfFirst {
                    it == defaultGeocoder.value
                }
            ]
        }
        assertContains(android.R.id.summary, expected)
    }
}
