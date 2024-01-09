package org.owntracks.android.e2e

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Duration.Companion.seconds
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.Parser
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.matchers.withActionIconDrawable
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@OptIn(ExperimentalUnsignedTypes::class)
@RunWith(AndroidJUnit4::class)
class IntentTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

    @Test
    fun given_an_application_instance_when_sending_a_send_location_intent_then_a_location_message_is_published_with_the_user_trigger() {
        setupTestActivity()

        initializeMockLocationProvider(app)
        setMockLocation(51.0, 0.0)

        packetReceivedIdlingResource.latch("\"t\":\"u\"")
        ContextCompat.startForegroundService(
            app,
            Intent(app, BackgroundService::class.java).apply {
                action = "org.owntracks.android.SEND_LOCATION_USER"
            }
        )
        packetReceivedIdlingResource.use(10.seconds) {
            Espresso.onIdle()
        }

        Assert.assertTrue(
            mqttPacketsReceived.also {
                Timber.v("MQTT Packets received: $it")
            }.filterIsInstance<MQTTPublish>().map {
                Parser(null).fromJson((it.payload)!!.toByteArray())
            }.also {
                Timber.w("packets: $it")
            }.any {
                it is MessageLocation && it.trigger == MessageLocation.ReportType.USER
            }
        )
    }

    @Test
    fun given_an_application_instance_when_sending_a_change_monitoring_intent_with_no_specific_mode_then_the_app_changes_monitoring_mode_to_the_next() {
        setupTestActivity()

        app.preferenceSetIdlingResource.setIdleState(false)
        ContextCompat.startForegroundService(
            app,
            Intent(app, BackgroundService::class.java).apply {
                action = "org.owntracks.android.CHANGE_MONITORING"
            }
        )
        app.preferenceSetIdlingResource.use {
            onView(ViewMatchers.withId(R.id.menu_monitoring))
                .check(ViewAssertions.matches(withActionIconDrawable(R.drawable.ic_step_forward_2)))
        }
    }

    @Test
    fun given_an_application_instance_when_sending_a_change_monitoring_intent_with_quiet_mode_specified_then_the_app_changes_monitoring_mode_to_quiet() {
        setupTestActivity()
        app.preferenceSetIdlingResource.setIdleState(false)
        ContextCompat.startForegroundService(
            app,
            Intent(app, BackgroundService::class.java).apply {
                action = "org.owntracks.android.CHANGE_MONITORING"
                putExtra("monitoring", -1)
            }
        )
        app.preferenceSetIdlingResource.use {
            onView(ViewMatchers.withId(R.id.menu_monitoring))
                .check(ViewAssertions.matches(withActionIconDrawable(R.drawable.ic_baseline_stop_36)))
        }
    }

    private fun setupTestActivity() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        configureMQTTConnectionToLocalWithGeneratedPassword()
        waitUntilActivityVisible<MapActivity>()
    }
}
