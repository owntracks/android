package org.owntracks.android.ui

import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.preferences.DefaultsProviderImpl
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.SharedPreferencesStore
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.scrollToPreferenceWithText
import org.owntracks.android.testutils.writeToPreference
import org.owntracks.android.ui.preferences.PreferencesActivity

@MediumTest
@HiltAndroidTest
class PreferencesActivityTests : TestWithAnActivity<PreferencesActivity>() {

  @Test
  fun initial_view_shows_top_level_menu() {
    assertDisplayed(R.string.preferencesServer)
    assertDisplayed(R.string.preferencesReporting)
    assertDisplayed(R.string.preferencesNotification)
    assertDisplayed(R.string.preferencesAdvanced)
    assertDisplayed(R.string.configurationManagement)
  }

  @Test
  fun setting_simple_mqtt_config_settings_can_be_shown_in_editor() {
    clickOn(R.string.preferencesServer)
    clickOn(R.string.mode_heading)
    clickOn(R.string.mode_mqtt_private_label)

    writeToPreference(R.string.preferencesHost, "mqtt.example.com")
    writeToPreference(R.string.preferencesPort, "1234")
    writeToPreference(
        R.string.preferencesClientId, "test-clientId") // This hyphen will get squelched

    scrollToPreferenceWithText(R.string.preferencesWebsocket)
    clickOn(R.string.preferencesWebsocket)

    writeToPreference(R.string.preferencesUsername, "testUsername")
    writeToPreference(R.string.preferencesBrokerPassword, "testPassword")
    writeToPreference(R.string.preferencesDeviceName, "testDeviceId")
    writeToPreference(R.string.preferencesTrackerId, "t5")

    scrollToPreferenceWithText(R.string.preferencesCleanSessionEnabled)
    clickOn(R.string.preferencesCleanSessionEnabled)

    writeToPreference(R.string.preferencesKeepalive, "1570")

    clickBack()
    clickOn(R.string.configurationManagement)

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
    assertContains(R.id.effectiveConfiguration, "\"tls\" : true")
    assertContains(R.id.effectiveConfiguration, "\"mqttProtocolLevel\" : 3")
    assertContains(R.id.effectiveConfiguration, "\"subTopic\" : \"owntracks/+/+\"")
    assertContains(R.id.effectiveConfiguration, "\"pubTopicBase\" : \"owntracks/%u/%d\"")
    assertContains(R.id.effectiveConfiguration, "\"ws\" : true")

    assertNotContains(R.id.effectiveConfiguration, "\"url\"")
    assertNotContains(R.id.effectiveConfiguration, "\"preferenceKeyDontReuseHttpClient\"")
  }

  @Test
  fun setting_simple_http_config_settings_can_be_shown_in_editor() {
    PreferenceManager.getDefaultSharedPreferences(app)
        .edit()
        .putBoolean(Preferences::notificationEvents.name, false)
        .apply()
    clickOn(R.string.preferencesServer)
    clickOn(R.string.mode_heading)
    clickOn(R.string.mode_http_private_label)

    writeToPreference(R.string.preferencesUrl, "https://www.example.com:8080/")

    writeToPreference(R.string.preferencesUsername, "testUsername")
    writeToPreference(R.string.preferencesBrokerPassword, "testPassword")
    writeToPreference(R.string.preferencesDeviceName, "testDeviceId")
    writeToPreference(R.string.preferencesTrackerId, "t1")
    clickBack()

    clickOn(R.string.preferencesReporting)
    clickOn(R.string.preferencesPubExtendedData)
    clickBack()

    clickOn(R.string.preferencesNotification)
    clickOn(R.string.preferencesNotificationEvents)

    clickBack()

    // This is an ugly hack, but there's some race conditions on underpowered hardware
    // causing the test to move on before the view has been fully built/rendered.

    clickOn(R.string.preferencesAdvanced)
    clickOn(R.string.preferencesRemoteCommand)
    writeToPreference(R.string.preferencesIgnoreInaccurateLocations, "950")
    writeToPreference(R.string.preferencesLocatorInterval, "123")
    writeToPreference(R.string.preferencesLocatorDisplacement, "567")
    writeToPreference(R.string.preferencesMoveModeLocatorInterval, "5")

    scrollToPreferenceWithText(R.string.preferencesPegLocatorFastestIntervalToInterval)
    clickOn(R.string.preferencesPegLocatorFastestIntervalToInterval)

    scrollToPreferenceWithText(R.string.preferencesAutostart)
    clickOn(R.string.preferencesAutostart)

    scrollToPreferenceWithText(R.string.preferencesReverseGeocodeProvider)
    clickOn(R.string.preferencesReverseGeocodeProvider)

    clickOn("OpenCage")
    clickOn(android.R.id.button1)

    writeToPreference(R.string.preferencesOpencageGeocoderApiKey, "geocodeAPIKey")

    clickBack()

    clickOn(R.string.configurationManagement)

    assertContains(R.id.effectiveConfiguration, "\"_type\" : \"configuration\"")
    assertContains(R.id.effectiveConfiguration, " \"waypoints\" : [ ]")

    assertContains(R.id.effectiveConfiguration, "\"url\" : \"https://www.example.com:8080/\"")
    assertContains(R.id.effectiveConfiguration, "\"username\" : \"testUsername\"")
    assertContains(R.id.effectiveConfiguration, "\"password\" : \"testPassword\"")
    assertContains(R.id.effectiveConfiguration, "\"deviceId\" : \"testDeviceId\"")
    assertContains(R.id.effectiveConfiguration, "\"tid\" : \"t1\"")
    assertContains(R.id.effectiveConfiguration, "\"notificationEvents\" : true")
    assertContains(R.id.effectiveConfiguration, "\"extendedData\" : false")
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
  fun default_geocoder_is_selected() {
    clickOn(R.string.preferencesAdvanced)
    scrollToPreferenceWithText(R.string.preferencesReverseGeocodeProvider)
    val defaultGeocoder =
        baristaRule.activityTestRule.activity.applicationContext.let {
          DefaultsProviderImpl()
              .getDefaultValue<ReverseGeocodeProvider>(
                  Preferences(
                      SharedPreferencesStore(
                          it, NotificationManagerCompat.from(app), NotificationsStash()),
                      SimpleIdlingResource("unused", true)),
                  Preferences::reverseGeocodeProvider)
        }
    val expected =
        baristaRule.activityTestRule.activity.resources.run {
          getStringArray(R.array.geocoders)[
              getStringArray(R.array.geocoderValues).indexOfFirst { it == defaultGeocoder.name }]
        }
    assertContains(android.R.id.summary, expected)
  }
}
