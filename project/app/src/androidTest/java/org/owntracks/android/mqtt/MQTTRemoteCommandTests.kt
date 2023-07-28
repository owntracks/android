package org.owntracks.android.mqtt

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.Parser
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.getPreferences
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class MQTTRemoteCommandTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {
    @Before
    @OptIn(DelicateCoroutinesApi::class)
    fun mqttBefore() {
        startBroker()
    }

    @After
    fun mqttAfter() {
        stopBroker()
    }

    @After
    fun uninitMockLocation() {
        unInitializeMockLocationProvider()
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_an_invalid_command_message_then_nothing_happens() {
        setNotFirstStartPreferences()
        launchActivity()

        grantMapActivityPermissions()

        configureMQTTConnectionToLocalWithGeneratedPassword()
        listOf(
            """
            {"_type":"cmd", "action":"invalid"}
            """.trimIndent()
        ).forEach {
            broker.publish(
                false,
                "owntracks/someuser/somedevice",
                Qos.AT_LEAST_ONCE,
                MQTT5Properties(),
                it.toByteArray().toUByteArray()
            )
        }
        // Wait for it to not crash
        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_reportLocation_command_message_then_a_response_location_is_sent_back_to_the_broker() { // ktlint-disable max-line-length
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()

        clickOnAndWait(R.id.menu_monitoring)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)
        configureMQTTConnectionToLocalWithGeneratedPassword()
        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
        listOf(
            """
            {"_type":"cmd", "action":"reportLocation"}
            """.trimIndent()
        ).forEach {
            broker.publish(
                false,
                "owntracks/$mqttUsername/$deviceId/cmd",
                Qos.AT_LEAST_ONCE,
                MQTT5Properties(),
                it.toByteArray().toUByteArray()
            )
        }

        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
            clickOnAndWait(R.id.fabMyLocation)
        }
        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            assertTrue(
                mqttPacketsReceived.filterIsInstance<MQTTPublish>().map {
                    Parser(null).fromJson((it.payload)!!.toByteArray())
                }.any {
                    it is MessageLocation &&
                        it.trigger == MessageLocation.ReportType.RESPONSE
                }
            )
        }
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_waypoints_response_message_then_a_waypoints_message_is_sent_back_to_the_broker() { // ktlint-disable max-line-length
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()

        clickOnAndWait(R.id.menu_monitoring)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)
        configureMQTTConnectionToLocalWithGeneratedPassword()
        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
        listOf(
            """
            {"_type":"cmd", "action":"waypoints"}
            """.trimIndent()
        ).forEach {
            broker.publish(
                false,
                "owntracks/$mqttUsername/$deviceId/cmd",
                Qos.AT_LEAST_ONCE,
                MQTT5Properties(),
                it.toByteArray().toUByteArray()
            )
        }

        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
            clickOnAndWait(R.id.fabMyLocation)
        }
        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            assertTrue(
                mqttPacketsReceived.filterIsInstance<MQTTPublish>().map {
                    Parser(null).fromJson((it.payload)!!.toByteArray())
                }.any {
                    it is MessageWaypoints && it.waypoints?.size == 2
                }
            )
        }
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_setConfiguration_response_message_then_the_configuration_is_set_in_the_preferences() { // ktlint-disable max-line-length
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        PreferenceManager.getDefaultSharedPreferences(baristaRule.activityTestRule.activity).edit {
            putBoolean("remoteConfiguration", true)
        }

        configureMQTTConnectionToLocalWithGeneratedPassword()
        baristaRule.activityTestRule.activity.importConfigurationIdlingResource.setIdleState(false)
        val testPreference = "TEST_PREFERENCE_VALUE"
        listOf(
            """
            {"_type":"cmd", "action":"setConfiguration", "configuration": {"_type":"configuration", "opencageApiKey": "$testPreference"}}
            """.trimIndent()
        ).forEach {
            broker.publish(
                false,
                "owntracks/$mqttUsername/$deviceId/cmd",
                Qos.AT_LEAST_ONCE,
                MQTT5Properties(),
                it.toByteArray().toUByteArray()
            )
        }
        baristaRule.activityTestRule.activity.importConfigurationIdlingResource.use {
            clickOnAndWait(R.id.fabMyLocation)
        }
        val configuredPreference = getPreferences().getString(
            Preferences::opencageApiKey.name,
            ""
        )
        assertEquals(testPreference, configuredPreference)
    }
}
