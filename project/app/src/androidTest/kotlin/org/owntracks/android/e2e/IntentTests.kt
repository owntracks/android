package org.owntracks.android.e2e

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.time.Duration.Companion.seconds
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.matchers.withActionIconDrawable
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@OptIn(ExperimentalUnsignedTypes::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IntentTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Test
  fun given_an_application_instance_when_sending_a_send_location_intent_then_a_location_message_is_published_with_the_user_trigger() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    waitUntilActivityVisible()
    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeMove)
    mockLocationProviderClient.setLocation(51.0, 0.0)

    mockLocationIdlingResource.use(15.seconds) { clickOn(R.id.fabMyLocation) }

    packetReceivedIdlingResource.latch("\"t\":\"u\"")
    ContextCompat.startForegroundService(
        app,
        Intent(app, BackgroundService::class.java).apply {
          action = "org.owntracks.android.SEND_LOCATION_USER"
        },
    )
    packetReceivedIdlingResource.use(10.seconds) { Espresso.onIdle() }

    Assert.assertTrue(
        mqttPacketsReceived
            .also { Timber.v("MQTT Packets received: $it") }
            .filterIsInstance<MQTTPublish>()
            .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
            .also { Timber.w("packets: $it") }
            .any {
              it is MessageLocation &&
                  it.trigger == MessageLocation.ReportType.USER &&
                  it.latitude == 51.0
            },
    )
  }

  @Test
  fun given_an_application_instance_when_sending_a_change_monitoring_intent_with_no_specific_mode_then_the_app_changes_monitoring_mode_to_the_next() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    ContextCompat.startForegroundService(
        app,
        Intent(app, BackgroundService::class.java).apply {
          action = "org.owntracks.android.CHANGE_MONITORING"
        },
    )

    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(
            ViewAssertions.matches(
                withActionIconDrawable(R.drawable.ic_baseline_pause_36),
            ),
        )
  }

  @Test
  fun given_an_application_instance_when_sending_a_change_monitoring_intent_with_quiet_mode_specified_then_the_app_changes_monitoring_mode_to_quiet() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    ContextCompat.startForegroundService(
        app,
        Intent(app, BackgroundService::class.java).apply {
          action = "org.owntracks.android.CHANGE_MONITORING"
          putExtra("monitoring", -1)
        },
    )

    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(
            ViewAssertions.matches(
                withActionIconDrawable(R.drawable.ic_baseline_stop_36),
            ),
        )
  }
}
