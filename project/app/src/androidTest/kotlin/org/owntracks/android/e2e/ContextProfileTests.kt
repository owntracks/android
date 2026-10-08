package org.owntracks.android.e2e

import android.app.Notification
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import javax.inject.Inject
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.location.profiles.ChargingSource
import org.owntracks.android.location.profiles.Condition
import org.owntracks.android.location.profiles.ContextProfile
import org.owntracks.android.location.profiles.ContextProfileManager
import org.owntracks.android.location.profiles.LocatorOverrides
import org.owntracks.android.location.profiles.StaticLocation
import org.owntracks.android.location.profiles.encodeContextProfiles
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.di.FakeDeviceContextProvider
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.matchers.withActionIconDrawable
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitUntilTrue
import org.owntracks.android.ui.map.MapActivity

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ContextProfileTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {
  @Inject lateinit var preferences: Preferences

  @Inject lateinit var device: FakeDeviceContextProvider

  @Inject lateinit var contextProfileManager: ContextProfileManager

  private val home =
      ContextProfile(
          "home",
          "Home",
          listOf(Condition.WifiSsid("Home")),
          // The tests start in quiet mode, which wouldn't publish anything
          LocatorOverrides(
              monitoring = MonitoringMode.Significant,
              staticLocation = StaticLocation(12.5, 34.25),
          ),
      )

  private val charging =
      ContextProfile(
          "charging",
          "Charging",
          listOf(Condition.Charging),
          LocatorOverrides(monitoring = MonitoringMode.Move),
      )

  private fun useProfiles(vararg profiles: ContextProfile) {
    preferences.contextProfiles = encodeContextProfiles(profiles.toList())
    preferences.contextProfilesEnabled = true
    // Changes to the device apply straight away, rather than once they've settled
    contextProfileManager.deviceContextSettleTime = Duration.ZERO
  }

  private fun publishedMessages(): List<MessageBase> =
      mqttPacketsReceived.filterIsInstance<MQTTPublish>().map {
        Parser(null).fromJson(it.payload!!.toByteArray())
      }

  @Test
  fun given_a_profile_with_a_static_location_when_it_applies_then_the_static_location_is_published_instead_of_real_ones() {
    useProfiles(home)
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    device.setWifi("Home")
    waitUntilTrue {
      publishedMessages().any {
        it is MessageLocation && it.latitude == 12.5 && it.longitude == 34.25
      }
    }

    mockLocationProviderClient.setLocation(51.0, 1.0)
    outgoingQueueIdlingResource.use { Espresso.onIdle() }
    assertFalse(
        "a real location isn't published",
        publishedMessages().any { it is MessageLocation && it.latitude == 51.0 },
    )
  }

  @Test
  fun given_a_profile_with_a_monitoring_mode_when_it_applies_then_that_mode_is_shown() {
    useProfiles(charging)
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }

    device.setCharging(ChargingSource.Cable)

    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(ViewAssertions.matches(withActionIconDrawable(R.drawable.ic_step_forward_2)))
    val notificationManager = app.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    waitUntilTrue {
      notificationManager.activeNotifications.any {
        it.notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT) ==
            app.getString(
                R.string.monitoringModeWithContextProfile,
                app.getString(R.string.monitoring_move),
                "Charging",
            )
      }
    }
  }

  @Test
  fun given_an_applied_profile_when_sending_a_change_monitoring_intent_then_the_mode_changes_and_the_profile_is_suspended() {
    useProfiles(charging)
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    }
    device.setCharging(ChargingSource.Cable)
    waitUntilTrue { contextProfileManager.activeProfile.value == charging }

    ContextCompat.startForegroundService(
        app,
        Intent(app, BackgroundService::class.java).apply {
          action = "org.owntracks.android.CHANGE_MONITORING"
          putExtra("monitoring", MonitoringMode.Quiet.value)
        },
    )

    onView(ViewMatchers.withId(R.id.menu_monitoring))
        .check(ViewAssertions.matches(withActionIconDrawable(R.drawable.ic_baseline_stop_36)))
    assertEquals(charging, contextProfileManager.matchedProfile.value)
    assertEquals("charging", preferences.suspendedContextProfileId)
  }
}
