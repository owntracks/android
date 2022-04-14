package org.owntracks.android.support

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.support.preferences.PreferencesStore
import org.owntracks.android.ui.NoopAppShortcuts

class PreferenceTest {
    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore

    @Before
    fun createMocks() {
        mockResources = PreferencesGettersAndSetters.getMockResources()
        mockContext = mock {
            on { resources } doReturn mockResources
            on { packageName } doReturn javaClass.canonicalName
        }
        preferencesStore = InMemoryPreferencesStore()
    }

    @Test
    fun `given a configuration message, when importing to preferences, all the keys in the config should be added`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["autostartOnBoot"] = true
        messageConfiguration["host"] = "testhost"
        messageConfiguration["port"] = 1234
        preferences.importFromMessage(messageConfiguration)
        assertEquals(true, preferences.autostartOnBoot)
        assertEquals("testhost", preferences.host)
        assertEquals(1234, preferences.port)
    }

    @Test
    fun `given a configuration message with an entry value of null, when importing to preferences, the config value should be cleared`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["host"] = null
        preferences.host = "testHost"
        preferences.importFromMessage(messageConfiguration)
        assertEquals("", preferences.host)
    }

    @Test
    fun `given a configuration message with an invalid key, when importing to preferences, it should be ignored`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["Invalid"] = "invalid"
        preferences.importFromMessage(messageConfiguration)
        assertFalse(preferences.exportToMessage().keys.contains("Invalid"))
    }

    @Test
    fun `given a configuration message with a value of the wrong type, when importing to preferences, it should be ignored`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["host"] = 4
        preferences.importFromMessage(messageConfiguration)
        assertEquals("", preferences.host)
    }

    private val preferenceKeys = listOf(
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
        "locatorPriority",
        "mode",
        "monitoring",
        "moveModeLocatorInterval",
        "notificationEvents",
        "notificationHigherPriority",
        "notificationLocation",
        "opencageApiKey",
        "password",
        "ping",
        "pubExtendedData",
        "cmd",
        "remoteConfiguration",
        "tid",
        "username",
        "showRegionsOnMap",
        "_build"
    )
    private val httpOnlyPreferenceKeys = listOf("dontReuseHttpClient", "url")
    private val mqttOnlyPreferenceKeys = listOf(
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
        "tlsCaCrt",
        "tlsClientCrt",
        "tlsClientCrtPassword"
    )

    @Test
    fun `given an MQTT configuration message, when imported and then exported, the config is merged and all the preference keys are present`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["autostartOnBoot"] = true
        messageConfiguration["host"] = "testhost"
        messageConfiguration["port"] = 1234
        messageConfiguration["mode"] = 0
        preferences.importFromMessage(messageConfiguration)

        val exportedMessageConfiguration = preferences.exportToMessage()
        preferenceKeys.forEach {
            assertTrue(
                "Exported message contains $it",
                exportedMessageConfiguration.containsKey(it)
            )
        }
        mqttOnlyPreferenceKeys.forEach {
            assertTrue(
                "Exported message contains $it",
                exportedMessageConfiguration.containsKey(it)
            )
        }
        httpOnlyPreferenceKeys.forEach {
            assertFalse(
                "Exported message doesn't contain $it",
                exportedMessageConfiguration.containsKey(it)
            )
        }
    }

    @Test
    fun `given an HTTP configuration message, when imported and then exported, the config is merged and all the preference keys are present`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["autostartOnBoot"] = true
        messageConfiguration["host"] = "testhost"
        messageConfiguration["port"] = 1234
        messageConfiguration["mode"] = 3
        preferences.importFromMessage(messageConfiguration)

        val exportedMessageConfiguration = preferences.exportToMessage()
        preferenceKeys.forEach {
            assertTrue(
                "Exported message contains $it",
                exportedMessageConfiguration.containsKey(it)
            )
        }
        httpOnlyPreferenceKeys.forEach {
            assertTrue(
                "Exported message contains $it",
                exportedMessageConfiguration.containsKey(it)
            )
        }
        mqttOnlyPreferenceKeys.forEach {
            assertFalse(
                "Exported message doesn't contain $it",
                exportedMessageConfiguration.containsKey(it)
            )
        }
    }

    @Test
    fun `given a Preferences object with no username set, when asking for the topic, the correct username placeholder is populated`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        preferences.username = ""
        preferences.deviceId = "myDevice"

        assertEquals("owntracks/user/myDevice", preferences.pubTopicLocations)

    }
}