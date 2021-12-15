package org.owntracks.android.e2e

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.PermissionGranter
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.owntracks.android.App
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.Parser
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.TimeUnit


@kotlin.ExperimentalUnsignedTypes
class MQTTTransitionEventTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithMQTTBrokerImpl(),
    MockDeviceLocation by FusedGMSockDeviceLocation() {
    private val uiDevice by lazy { UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) }

    @After
    fun closeNotifications() {
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext.applicationContext.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
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

    @After
    fun unregisterIdlingResource() {
        try {
            IdlingRegistry.getInstance()
                .unregister(baristaRule.activityTestRule.activity.locationIdlingResource)
        } catch (_: NullPointerException) {
            // Happens when the vm is already gone from the MapActivity
        }
        try {
            IdlingRegistry.getInstance()
                .unregister(baristaRule.activityTestRule.activity.outgoingQueueIdlingResource)
        } catch (_: NullPointerException) {
        }
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_transition_message_then_something_happens() {
        setNotFirstStartPreferences()
        baristaRule.launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)

        clearNotifications()
        configureMQTTConnectionToLocal()

        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource as SimpleIdlingResource?)

        listOf(
            MessageLocation().apply {
                latitude = 52.123
                longitude = 0.56789
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            }, MessageTransition().apply {
                accuracy = 48f
                description = "Transition!"
                event = "enter"
                latitude = 52.12
                longitude = 0.56
                trigger = "l"
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            }
        )
            .map(Parser(null)::toJsonBytes)
            .forEach {
                broker.publish(
                    false,
                    "owntracks/someuser/somedevice",
                    Qos.AT_LEAST_ONCE,
                    MQTT5Properties(),
                    it.toUByteArray()
                )
            }
        uiDevice.openNotification()
        uiDevice.wait(
            Until.hasObject(By.textStartsWith("2006-01-02")),
            TimeUnit.SECONDS.toMillis(30)
        )
        val notification =
            uiDevice.findObject(By.textStartsWith("2006-01-02 15:04 ce enters Transition!"))
        assertNotNull(notification)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun given_an_MQTT_configured_client_when_the_location_enters_a_geofence_a_transition_message_is_sent_and_notification_raised() {
        setNotFirstStartPreferences()
        baristaRule.launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)

        initializeMockLocationProvider(
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.applicationContext
        )
        val regionLatitude = 48.0
        val regionLongitude = -1.0
        val regionDescription = "Test Region"

        clearNotifications()
        configureMQTTConnectionToLocal()

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)
        setMockLocation(51.0, 0.0)
        IdlingPolicies.setIdlingResourceTimeout(1, TimeUnit.MINUTES)
        (baristaRule.activityTestRule.activity.locationIdlingResource as SimpleIdlingResource?).run {
            IdlingRegistry.getInstance().register(this)
        }
        clickOnAndWait(R.id.menu_mylocation)
        clickOnAndWait(R.id.menu_report)

        openDrawer()
        clickOnAndWait(R.string.title_activity_regions)
        clickOnAndWait(R.id.add)

        writeTo(R.id.description, regionDescription)
        writeTo(R.id.latitude, regionLatitude.toString())
        writeTo(R.id.longitude, regionLongitude.toString())
        writeTo(R.id.radius, "100")

        clickOnAndWait(R.id.save)

        openDrawer()
        setMockLocation(51.0, 0.0)
        clickOnAndWait(R.string.title_activity_map)

        setMockLocation(regionLatitude, regionLongitude)
        clickOnAndWait(R.id.menu_mylocation)
        clickOnAndWait(R.id.menu_report)

        uiDevice.openNotification()

        val notification = uiDevice.findObject(By.textEndsWith(regionDescription))
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        baristaRule.activityTestRule.activity.sendBroadcast(closeIntent)
        assertNotNull(notification)
        assertTrue(notification.text.endsWith("$deviceId enters Test Region"))

        assertTrue("Packet has been received that is a transition message with the correct details",
            mqttPacketsReceived
                .filterIsInstance<MQTTPublish>()

                .map {
                    Pair(it.topicName, Parser(null).fromJson((it.payload)!!.toByteArray()))
                }
                .any {
                    it.second.let { message ->
                        message is MessageTransition
                                && message.description == regionDescription
                                && message.latitude == regionLatitude
                                && message.longitude == regionLongitude
                                && message.event == "enter"
                    } && it.first == "owntracks/$mqttUsername/$deviceId/event"
                })
    }

    private fun clearNotifications() {
        // Cancelling the notification doesn't actually fire the delete intent, so we need to do this manually
        (InstrumentationRegistry
            .getInstrumentation()
            .targetContext.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        baristaRule.activityTestRule.activity.startService(
            Intent(baristaRule.activityTestRule.activity, BackgroundService::class.java).setAction(
                BackgroundService.INTENT_ACTION_CLEAR_NOTIFICATIONS
            )
        )
    }
}