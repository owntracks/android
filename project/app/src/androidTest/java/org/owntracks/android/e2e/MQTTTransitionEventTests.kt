package org.owntracks.android.e2e

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.support.Parser
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@ExperimentalUnsignedTypes
@LargeTest
@RunWith(AndroidJUnit4::class)
class MQTTTransitionEventTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {
    private val uiDevice by lazy {
        UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation()
        )
    }

    @DelicateCoroutinesApi
    @Before
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

    @Before
    fun clearNotifications() {
        // Cancel notifications
        (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        // Close the notification shade
        @Suppress("DEPRECATION")
        app.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    @Before
    fun clearLocalData() {
        app.filesDir.listFiles()
            ?.forEach { it.delete(); }
    }

    @Before
    fun stopAndroidSetup() {
        stopAndroidSetupProcess()
        disableHeadsupNotifications()
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_transition_message_then_a_notification_appears() { // ktlint-disable max-line-length
        setNotFirstStartPreferences()
        launchActivity()

        allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)

        configureMQTTConnectionToLocalWithGeneratedPassword()

        app.mqttConnectionIdlingResource.with {
            reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource)
        }

        listOf(
            MessageLocation().apply {
                latitude = 52.123
                longitude = 0.56789
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
            MessageTransition().apply {
                accuracy = 48f
                description = "Transition!"
                event = "enter"
                latitude = 52.12
                longitude = 0.56
                trigger = "l"
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            }
        ).map(Parser(null)::toJsonBytes)
            .forEach {
                broker.publish(
                    false,
                    "owntracks/someuser/somedevice",
                    Qos.AT_LEAST_ONCE,
                    MQTT5Properties(),
                    it.toUByteArray()
                )
            }
        val success = uiDevice.openNotification()
        assertTrue(success)
        uiDevice.wait(
            Until.hasObject(By.textStartsWith("2006-01-02")),
            TimeUnit.SECONDS.toMillis(30)
        )
        val notification = uiDevice.findObject(By.textStartsWith("2006-01-02 15:04 ce enters Transition!"))
        assertNotNull(notification)
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_location_enters_a_geofence_a_transition_message_is_sent() { // ktlint-disable max-line-length
        setNotFirstStartPreferences()
        launchActivity()
        allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)

        initializeMockLocationProvider(app)
        val waypointLatitude = 48.0
        val waypointLongitude = -1.0
        val waypointDescription = "Test Region"

        configureMQTTConnectionToLocalWithGeneratedPassword()
        waitUntilActivityVisible<MapActivity>()

        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource) {
            setMockLocation(51.0, 0.0)
        }

        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)
        clickOnAndWait(R.id.add)

        writeTo(R.id.description, waypointDescription)
        writeTo(R.id.latitude, waypointLatitude.toString())
        writeTo(R.id.longitude, waypointLongitude.toString())
        writeTo(R.id.radius, "100")

        clickOnAndWait(R.id.save)

        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource) {
            setMockLocation(waypointLatitude, waypointLongitude)
        }

        assertTrue(
            "Packet has been received that is a transition message with the correct details",
            mqttPacketsReceived.filterIsInstance<MQTTPublish>()
                .map {
                    Pair(it.topicName, Parser(null).fromJson((it.payload)!!.toByteArray()))
                }
                .any {
                    it.second.let { message ->
                        message is MessageTransition && message.description == waypointDescription && message.latitude == waypointLatitude && message.longitude == waypointLongitude && message.event == "enter"
                    } && it.first == "owntracks/$mqttUsername/$deviceId/event"
                }
        )
    }
}
