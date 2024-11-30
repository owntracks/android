package org.owntracks.android.waypoints

import android.Manifest.permission.POST_NOTIFICATIONS
import androidx.test.espresso.Espresso
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.minutes
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.owntracks.android.R
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.waypoints.WaypointsActivity

@MediumTest
@RunWith(Parameterized::class)
@HiltAndroidTest
@Ignore("Needs git LFS for the moment, which doesn't work on CI")
class WaypointsMigrationFromObjectboxTestWithFullUI(private val parameter: Parameter) :
    TestWithAnActivity<WaypointsActivity>(false) {

  @Inject
  @Named("waypointsMigrationIdlingResource")
  lateinit var migrationIdlingResource: SimpleIdlingResource

  @Inject
  @Named("waypointsEventCountingIdlingResource")
  lateinit var waypointsEventCountingIdlingResource: ThresholdIdlingResourceInterface

  @Inject
  @Named("waypointsRecyclerViewIdlingResource")
  lateinit var waypointsRecyclerViewIdlingResource: ThresholdIdlingResourceInterface

  @Before
  fun clearLocalPackageData() {
    InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("waypoints")
  }

  private fun setupActivity(dataBytes: ByteArray) {
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .filesDir
        .resolve("objectbox/objectbox/")
        .run {
          mkdirs()
          resolve("data.mdb").run { outputStream().use { it.write(dataBytes) } }
        }
    setNotFirstStartPreferences()
    launchActivity()
    allowPermissionsIfNeeded(POST_NOTIFICATIONS)
    waitUntilActivityVisible()
  }

  @Test
  fun migratingAnObjectboxDisplaysCorrectNumberOfWaypointsInActivity() {
    waypointsEventCountingIdlingResource.set(0)
    waypointsEventCountingIdlingResource.threshold = parameter.expectedCount
    waypointsRecyclerViewIdlingResource.set(0)
    waypointsRecyclerViewIdlingResource.threshold = parameter.expectedCount
    val dataBytes =
        this.javaClass.getResource("/objectbox-lmdbs/${parameter.dbName}/data.mdb")!!.readBytes()
    setupActivity(dataBytes)

    migrationIdlingResource.use { Espresso.onIdle() }
    waypointsEventCountingIdlingResource.use { Espresso.onIdle() }
    waypointsRecyclerViewIdlingResource.use(1.minutes) {
      if (parameter.expectedCount == 0) {
        assertDisplayed(R.id.placeholder)
      } else {
        assertDisplayed(R.id.waypointsRecyclerView)
        assertRecyclerViewItemCount(R.id.waypointsRecyclerView, parameter.expectedCount)
      }
    }
  }

  data class Parameter(val dbName: String, val expectedCount: Int) {
    override fun toString(): String = dbName
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Iterable<Parameter> {
      return arrayListOf(
          Parameter("single-waypoint", 1),
          Parameter("10-waypoints", 10),
          Parameter("5000-waypoints", 5000),
          Parameter("empty", 0),
          Parameter("3-created-by-real-device", 3),
      )
    }
  }
  //  @Test
  //  @Ignore
  //  fun migratingACorruptObjectboxDatabaseGivesNoWaypointsAndANotification() {
  //    val random = Random(1)
  //    setupActivity(random.nextBytes(4096))
  //
  //    migrationIdlingResource.use { assertNotDisplayed(R.id.waypointsRecyclerView) }
  //
  //    val notificationManager =
  //        app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  //    Assert.assertTrue(
  //        "Event notification is displayed",
  //        notificationManager.activeNotifications
  //            .map { it.notification }
  //            .also { notifications ->
  //              notifications
  //                  .map { it.extras.getString(Notification.EXTRA_TITLE) }
  //                  .run { Timber.i("Current Notifications: $this") }
  //            }
  //            .any { notification ->
  //              notification.extras.getString(Notification.EXTRA_TITLE) ==
  //                  "Error migrating waypoints" &&
  //                  notification.extras.getString(Notification.EXTRA_TEXT) ==
  //                      "An error occurred whilst migrating waypoints. Some may not have been
  // migrated, so check the logs and re-add."
  //            })
  //  }
}
