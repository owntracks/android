package org.owntracks.android.ui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.getPreferences
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@MediumTest
@HiltAndroidTest
class TLSCertMigrationNotificationTests : TestWithAnActivity<MapActivity>(false) {

  @Test
  fun when_migrating_a_legacy_tls_ca_preference_then_the_notification_is_shown() {
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSharedPreferences("org.owntracks.android.preferences.private", Context.MODE_PRIVATE)
        .edit()
        .putString("tlsCaCrt", "TestValue")
        .apply()
    setNotFirstStartPreferences()
    app.migratePreferences()
    launchActivity()
    grantMapActivityPermissions()

    val notificationManager =
        app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    Assert.assertTrue(
        "Event notification is displayed",
        notificationManager.activeNotifications
            .map { it.notification }
            .also { notifications ->
              notifications
                  .map { it.extras.getString(Notification.EXTRA_TITLE) }
                  .run { Timber.i("Current Notifications: $this") }
            }
            .any { notification ->
              notification.extras.getString(Notification.EXTRA_TITLE) ==
                  app.getString(R.string.certificateMigrationRequiredNotificationTitle) &&
                  notification.extras.getString(Notification.EXTRA_BIG_TEXT) ==
                      app.getString(R.string.certificateMigrationRequiredNotificationText)
            })
  }

  @Test
  fun when_migrating_a_current_tls_ca_preference_then_the_notification_is_shown() {
    getPreferences().edit().putString("tlsCaCrt", "TestValue").apply()
    setNotFirstStartPreferences()
    app.migratePreferences()
    launchActivity()
    grantMapActivityPermissions()

    val notificationManager =
        app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    Assert.assertTrue(
        "Event notification is displayed",
        notificationManager.activeNotifications
            .map { it.notification }
            .also { notifications ->
              notifications
                  .map { it.extras.getString(Notification.EXTRA_TITLE) }
                  .run { Timber.i("Current Notifications: $this") }
            }
            .any { notification ->
              notification.extras.getString(Notification.EXTRA_TITLE) ==
                  app.getString(R.string.certificateMigrationRequiredNotificationTitle) &&
                  notification.extras.getString(Notification.EXTRA_BIG_TEXT) ==
                      app.getString(R.string.certificateMigrationRequiredNotificationText)
            })
  }
}
