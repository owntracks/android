package org.owntracks.android.mqtt

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import java.util.concurrent.TimeUnit
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class MQTTRemoteCommandTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_an_invalid_command_message_then_nothing_happens() {
        setupTestActivity()

        listOf(
            //language=JSON
            """
            {
              "_type": "cmd",
              "action": "invalid"
            }
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
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_reportLocation_command_message_then_a_response_location_is_sent_back_to_the_broker() {
        setupTestActivity()
        initializeMockLocationProvider(app)
        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(52.0, 0.0)
        }
        clickOnAndWait(R.id.menu_monitoring)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)

        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
        listOf(
            //language=JSON
            """
            {
              "_type": "cmd",
              "action": "reportLocation"
            }
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
                    it is MessageLocation && it.trigger == MessageLocation.ReportType.RESPONSE
                }
            )
        }
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_waypoints_response_message_then_a_waypoints_message_is_sent_back_to_the_broker() {
        setupTestActivity()

        clickOnAndWait(R.id.menu_monitoring)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)

        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "test waypoint")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.123")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0.456")
        BaristaEditTextInteractions.writeTo(R.id.radius, "20")
        clickOnAndWait(R.id.save)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "test waypoint 2")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.00")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0.4")
        BaristaEditTextInteractions.writeTo(R.id.radius, "25")
        clickOnAndWait(R.id.save)

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)

        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
        listOf(
            //language=JSON
            """
            {
              "_type": "cmd",
              "action": "waypoints"
            }
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
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_set_waypoints_command_message_then_the_waypoints_are_merged_with_the_existing_waypoints() {
        setupTestActivity()

        clickOnAndWait(R.id.menu_monitoring)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)

        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "test waypoint")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.123")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0.456")
        BaristaEditTextInteractions.writeTo(R.id.radius, "20")
        clickOnAndWait(R.id.save)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "test waypoint 2")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.00")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0.4")
        BaristaEditTextInteractions.writeTo(R.id.radius, "25")
        clickOnAndWait(R.id.save)

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)

        listOf(
            //language=JSON
            """
  {
    "_type": "cmd",
    "action": "setWaypoints",
    "waypoints": {
      "_type": "waypoints",
      "waypoints": [
        {
          "_type": "waypoint",
          "desc": "work",
          "lat": 51.5,
          "lon": -0.02,
          "rad": 150,
          "tst": 1505910709000
        },
        {
          "_type": "waypoint",
          "desc": "home",
          "lat": 53.6,
          "lon": -1.5,
          "rad": 100,
          "tst": 1558351273
        }
      ]
    }
  }
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
        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)
        assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 4)
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_clear_waypoints_command_message_then_the_waypoints_are_cleared() {
        setupTestActivity()

        clickOnAndWait(R.id.menu_monitoring)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)

        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "test waypoint")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.123")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0.456")
        BaristaEditTextInteractions.writeTo(R.id.radius, "20")
        clickOnAndWait(R.id.save)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "test waypoint 2")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.00")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0.4")
        BaristaEditTextInteractions.writeTo(R.id.radius, "25")
        clickOnAndWait(R.id.save)

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)

        listOf(
            //language=JSON
            """
  {
    "_type": "cmd",
    "action": "clearWaypoints"
  }
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
        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)
        assertNotDisplayed(R.id.waypointsRecyclerView)
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_setConfiguration_response_message_then_the_configuration_is_set_in_the_preferences() {
        setupTestActivity()
        PreferenceManager.getDefaultSharedPreferences(baristaRule.activityTestRule.activity).edit {
            putBoolean("remoteConfiguration", true)
        }
        baristaRule.activityTestRule.activity.importConfigurationIdlingResource.setIdleState(false)
        val testPreference = "TEST_PREFERENCE_VALUE"

        listOf(
            //language=JSON
            """
            {
              "_type": "cmd",
              "action": "setConfiguration",
              "configuration": {
                "_type": "configuration",
                "opencageApiKey": "$testPreference"
              }
            }
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

    @Test
    fun given_an_MQTT_configured_client_to_not_accept_remote_configs_when_the_broker_sends_a_setConfiguration_response_message_then_the_configuration_is_not_set_in_the_preferences() {
        setupTestActivity()
        PreferenceManager.getDefaultSharedPreferences(baristaRule.activityTestRule.activity).edit {
            putBoolean("remoteConfiguration", false)
        }
        baristaRule.activityTestRule.activity.importConfigurationIdlingResource.setIdleState(false)
        val testPreference = "TEST_PREFERENCE_VALUE"

        listOf(
            //language=JSON
            """
            {
              "_type": "cmd",
              "action": "setConfiguration",
              "configuration": {
                "_type": "configuration",
                "opencageApiKey": "$testPreference"
              }
            }
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

        assertEquals("", configuredPreference)
    }

    private fun setupTestActivity() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        configureMQTTConnectionToLocalWithGeneratedPassword()
        waitUntilActivityVisible<MapActivity>()
        waitForMQTTToCompleteAndContactsToBeCleared()
    }
}
