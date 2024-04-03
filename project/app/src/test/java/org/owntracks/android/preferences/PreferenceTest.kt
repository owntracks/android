package org.owntracks.android.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.support.SimpleIdlingResource

class PreferenceTest {
  private lateinit var mockContext: Context
  private lateinit var preferencesStore: PreferencesStore

  @Before
  fun createMocks() {
    mockContext = mock { on { packageName } doReturn javaClass.canonicalName }
    preferencesStore = InMemoryPreferencesStore()
  }

  private val mockIdlingresource = SimpleIdlingResource("mock", true)

  @Test
  fun `given a single key value, when importing to preferences, then that value can be retrieved from the preferences`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    preferences.importKeyValue("ignoreStaleLocations", 195.4f)
    assertEquals(195.4f, preferences.ignoreStaleLocations, 0.001f)
  }

  @Test
  fun `given a configuration message, when importing to preferences, all the keys in the config should be added`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["autostartOnBoot"] = true
    messageConfiguration["host"] = "testhost"
    messageConfiguration["port"] = 1234
    preferences.importConfiguration(messageConfiguration)
    assertEquals(true, preferences.autostartOnBoot)
    assertEquals("testhost", preferences.host)
    assertEquals(1234, preferences.port)
  }

  @Test
  fun `given a configuration message with an entry value of null, when importing to preferences, the config value should be cleared`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["host"] = null
    preferences.host = "testHost"
    preferences.importConfiguration(messageConfiguration)
    assertEquals("", preferences.host)
  }

  @Test
  fun `given a configuration message with an invalid key, when importing to preferences, it should be ignored`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["Invalid"] = "invalid"
    preferences.importConfiguration(messageConfiguration)
    assertFalse(preferences.exportToMessage().keys.contains("Invalid"))
  }

  @Test
  fun `given a configuration message with a value of the wrong type, when importing to preferences, it should be ignored`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["host"] = 4
    preferences.importConfiguration(messageConfiguration)
    assertEquals("", preferences.host)
  }

  private val preferenceKeys =
      listOf(
          "autostartOnBoot",
          "connectionTimeoutSeconds",
          "debugLog",
          "deviceId",
          "fusedRegionDetection",
          "reverseGeocodeProvider",
          "ignoreInaccurateLocations",
          "ignoreStaleLocations",
          "locatorDisplacement",
          "locatorInterval",
          "mode",
          "monitoring",
          "moveModeLocatorInterval",
          "notificationEvents",
          "notificationHigherPriority",
          "notificationLocation",
          "opencageApiKey",
          "password",
          "ping",
          "extendedData",
          "cmd",
          "remoteConfiguration",
          "tid",
          "username",
          "showRegionsOnMap",
          "_build")
  private val httpOnlyPreferenceKeys = listOf("dontReuseHttpClient", "url")
  private val mqttOnlyPreferenceKeys =
      listOf(
          "cleanSession",
          "clientId",
          "host",
          "info",
          "keepalive",
          "mqttProtocolLevel",
          "pubQos",
          "pubRetain",
          "sub",
          "subQos",
          "subTopic",
          "port",
          "pubTopicBase",
          "tls",
          "tlsClientCrt")

  @Test
  fun `given an MQTT configuration message containing remapped values, when imported and exported the remapped values are present`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["pubExtendedData"] = true

    preferences.importConfiguration(messageConfiguration)

    val exportedMessageConfiguration = preferences.exportToMessage()
    assertTrue(exportedMessageConfiguration[Preferences::extendedData.name] as Boolean)
  }

  @Test
  fun `given an MQTT configuration message, when imported and then exported, the config is merged and all the preference keys are present`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["autostartOnBoot"] = true
    messageConfiguration["host"] = "testhost"
    messageConfiguration["port"] = 1234
    messageConfiguration["mode"] = 0
    preferences.importConfiguration(messageConfiguration)

    val exportedMessageConfiguration = preferences.exportToMessage()
    preferenceKeys.forEach {
      assertTrue("Exported message contains $it", exportedMessageConfiguration.containsKey(it))
    }
    mqttOnlyPreferenceKeys.forEach {
      assertTrue("Exported message contains $it", exportedMessageConfiguration.containsKey(it))
    }
    httpOnlyPreferenceKeys.forEach {
      assertFalse(
          "Exported message doesn't contain $it", exportedMessageConfiguration.containsKey(it))
    }
  }

  @Test
  fun `given an HTTP configuration message, when imported and then exported, the config is merged and all the preference keys are present`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration["autostartOnBoot"] = true
    messageConfiguration["host"] = "testhost"
    messageConfiguration["port"] = 1234
    messageConfiguration["mode"] = 3
    preferences.importConfiguration(messageConfiguration)

    val exportedMessageConfiguration = preferences.exportToMessage()
    preferenceKeys.forEach {
      assertTrue("Exported message contains $it", exportedMessageConfiguration.containsKey(it))
    }
    httpOnlyPreferenceKeys.forEach {
      assertTrue("Exported message contains $it", exportedMessageConfiguration.containsKey(it))
    }
    mqttOnlyPreferenceKeys.forEach {
      assertFalse(
          "Exported message doesn't contain $it", exportedMessageConfiguration.containsKey(it))
    }
  }

  @Test
  fun `given a Preferences object with no username set, when asking for the topic, the correct username placeholder is populated`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    preferences.autostartOnBoot = true
    preferences.username = ""
    preferences.deviceId = "myDevice"

    assertEquals("owntracks/user/myDevice", preferences.pubTopicLocations)
  }

  @Test
  fun `given an empty Preferences object, when asking for a value, then the default value is returned`() {
    val preferences = Preferences(preferencesStore, mockIdlingresource)
    assertEquals(false, preferences.debugLog)
  }
}
