package org.owntracks.android.e2e

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.receiver.ExternalIntentReceiver
import org.owntracks.android.testutils.JustThisTestPlease
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.matchers.withActionIconDrawable
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitAndClickWithMinVisibility
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalUnsignedTypes::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@JustThisTestPlease
class IntentTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Test
  fun given_an_application_instance_when_sending_a_send_location_intent_then_a_location_message_is_published_with_the_user_trigger() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    waitUntilActivityVisible()
    clickOn(R.id.menu_monitoring)
    waitAndClickWithMinVisibility(R.id.fabMonitoringModeMove)
    mockLocationProviderClient.setLocation(51.0, 1.0)

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
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

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
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

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

  @Test
  fun given_intent_control_enabled_when_change_monitoring_sent_to_external_receiver_then_monitoring_mode_changes() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    val prefs = PreferenceManager.getDefaultSharedPreferences(app)
    prefs.edit().putBoolean(Preferences::allowIntentControl.name, true).commit()
    val authKey =
        prefs.getString(Preferences::intentAuthKey.name, null)
            ?: throw IllegalStateException("intentAuthKey not set")

    // setupTestActivity starts in Quiet (stop icon); cycling should advance to Manual (pause icon)
    app.sendBroadcast(
        Intent(app, ExternalIntentReceiver::class.java).apply {
          action = BackgroundService.INTENT_ACTION_CHANGE_MONITORING
          putExtra(ExternalIntentReceiver.INTENT_EXTRA_AUTH_KEY, authKey)
        })

    // Allow time for broadcast delivery → receiver → service → UI update
    Thread.sleep(500)

    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(
            ViewAssertions.matches(
                withActionIconDrawable(R.drawable.ic_baseline_pause_36),
            ),
        )
  }

  @Test
  fun given_intent_control_enabled_when_change_monitoring_sent_with_wrong_key_then_monitoring_mode_does_not_change() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    PreferenceManager.getDefaultSharedPreferences(app)
        .edit()
        .putBoolean(Preferences::allowIntentControl.name, true)
        .commit()

    app.sendBroadcast(
        Intent(app, ExternalIntentReceiver::class.java).apply {
          action = BackgroundService.INTENT_ACTION_CHANGE_MONITORING
          putExtra(ExternalIntentReceiver.INTENT_EXTRA_AUTH_KEY, "wrong-key")
        })

    Thread.sleep(500)

    // Monitoring should remain Quiet (stop icon) — wrong key rejected
    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(
            ViewAssertions.matches(
                withActionIconDrawable(R.drawable.ic_baseline_stop_36),
            ),
        )
  }

  @Test
  fun given_intent_control_disabled_when_change_monitoring_sent_to_external_receiver_then_monitoring_mode_does_not_change() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    // allowIntentControl defaults to false — no explicit set needed
    app.sendBroadcast(
        Intent(app, ExternalIntentReceiver::class.java).apply {
          action = BackgroundService.INTENT_ACTION_CHANGE_MONITORING
        })

    // Give the broadcast time to be delivered and rejected by the receiver
    Thread.sleep(500)

    // Monitoring should remain Quiet (stop icon)
    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(
            ViewAssertions.matches(
                withActionIconDrawable(R.drawable.ic_baseline_stop_36),
            ),
        )
  }

  @Test
  fun given_intent_control_enabled_when_send_location_sent_to_external_receiver_then_location_message_is_published() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    val prefs = PreferenceManager.getDefaultSharedPreferences(app)
    prefs.edit().putBoolean(Preferences::allowIntentControl.name, true).commit()
    val authKey =
        prefs.getString(Preferences::intentAuthKey.name, null)
            ?: throw IllegalStateException("intentAuthKey not set")

    waitUntilActivityVisible()
    clickOn(R.id.menu_monitoring)
    waitAndClickWithMinVisibility(R.id.fabMonitoringModeMove)
    mockLocationProviderClient.setLocation(51.0, 1.0)

    mockLocationIdlingResource.use(15.seconds) { clickOn(R.id.fabMyLocation) }

    packetReceivedIdlingResource.latch("\"t\":\"u\"")
    app.sendBroadcast(
        Intent(app, ExternalIntentReceiver::class.java).apply {
          action = BackgroundService.INTENT_ACTION_SEND_LOCATION_USER
          putExtra(ExternalIntentReceiver.INTENT_EXTRA_AUTH_KEY, authKey)
        })
    packetReceivedIdlingResource.use(10.seconds) { Espresso.onIdle() }

    Assert.assertTrue(
        mqttPacketsReceived
            .also { Timber.v("MQTT Packets received: $it") }
            .filterIsInstance<MQTTPublish>()
            .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
            .any {
              it is MessageLocation &&
                  it.trigger == MessageLocation.ReportType.USER &&
                  it.latitude == 51.0
            },
    )
  }

  @Test
  fun given_intent_control_disabled_when_send_location_sent_to_external_receiver_then_no_location_message_is_published() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    waitUntilActivityVisible()
    clickOn(R.id.menu_monitoring)
    waitAndClickWithMinVisibility(R.id.fabMonitoringModeMove)
    mockLocationProviderClient.setLocation(51.0, 1.0)

    mockLocationIdlingResource.use(15.seconds) { clickOn(R.id.fabMyLocation) }

    val packetsBeforeBroadcast = mqttPacketsReceived.size

    // allowIntentControl defaults to false — broadcast should be ignored by the receiver
    app.sendBroadcast(
        Intent(app, ExternalIntentReceiver::class.java).apply {
          action = BackgroundService.INTENT_ACTION_SEND_LOCATION_USER
        })

    // Give the broadcast time to be delivered and rejected
    Thread.sleep(2000)

    Assert.assertFalse(
        "Expected no USER-triggered location message after ignored broadcast",
        mqttPacketsReceived
            .drop(packetsBeforeBroadcast)
            .filterIsInstance<MQTTPublish>()
            .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
            .any { it is MessageLocation && it.trigger == MessageLocation.ReportType.USER },
    )
  }
}
