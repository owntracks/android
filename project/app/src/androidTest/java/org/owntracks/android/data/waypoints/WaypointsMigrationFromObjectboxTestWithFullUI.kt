package org.owntracks.android.data.waypoints

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import kotlin.random.Random
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.RecyclerViewLayoutCompleteIdlingResource
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.waypoints.WaypointsActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class WaypointsMigrationFromObjectboxTestWithFullUI : TestWithAnActivity<WaypointsActivity>(
    WaypointsActivity::class.java,
    false
) {

    @Test
    fun migratingAnEmptyObjectboxProducesZeroWaypoints() {
        setNotFirstStartPreferences()
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/empty/data.mdb")!!.readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(dataBytes)
                }
            }
        }
        setNotFirstStartPreferences()
        launchActivity()
        waitUntilActivityVisible<WaypointsActivity>()
        app.migrateWaypoints()
        app.migrationIdlingResource.use {
            Espresso.onIdle()
        }
        assertNotDisplayed(R.id.waypointsRecyclerView)
    }

    @Test
    fun migratingAnObjectboxWithSinglePointProducesOneWaypoint() {
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/single-waypoint/data.mdb")!!.readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(dataBytes)
                }
            }
        }
        setNotFirstStartPreferences()
        launchActivity()
        val waypointsActivityIdlingResource = RecyclerViewLayoutCompleteIdlingResource(
            baristaRule.activityTestRule.activity
        )
        waitUntilActivityVisible<WaypointsActivity>()
        waypointsActivityIdlingResource.setUnidle()
        app.migrateWaypoints()
        app.migrationIdlingResource.use {
            Espresso.onIdle()
        }
        waypointsActivityIdlingResource.use {
            assertDisplayed(R.id.waypointsRecyclerView)
            assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 1)
        }
    }

    @Test
    fun migratingAnObjectboxWith10PointsProduces10Waypoints() {
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/10-waypoints/data.mdb")!!.readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(dataBytes)
                }
            }
        }

        setNotFirstStartPreferences()
        launchActivity()
        val waypointsActivityIdlingResource = RecyclerViewLayoutCompleteIdlingResource(
            baristaRule.activityTestRule.activity
        )
        waitUntilActivityVisible<WaypointsActivity>()
        waypointsActivityIdlingResource.setUnidle()
        app.migrateWaypoints()
        app.migrationIdlingResource.use {
            Espresso.onIdle()
        }
        waypointsActivityIdlingResource.use {
            assertDisplayed(R.id.waypointsRecyclerView)
            assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 10)
        }
    }

    @Test
    fun migratingAnObjectboxWith5000PointsProduces5000Waypoints() {
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/5000-waypoints/data.mdb")!!.readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(dataBytes)
                }
            }
        }
        setNotFirstStartPreferences()
        launchActivity()
        val waypointsActivityIdlingResource = RecyclerViewLayoutCompleteIdlingResource(
            baristaRule.activityTestRule.activity
        )
        waitUntilActivityVisible<WaypointsActivity>()
        waypointsActivityIdlingResource.setUnidle()
        app.migrateWaypoints()
        app.migrationIdlingResource.use {
            Espresso.onIdle()
        }
        waypointsActivityIdlingResource.use {
            assertDisplayed(R.id.waypointsRecyclerView)
            assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 5000)
        }
    }

    @Test
    fun migratingACorruptObjectboxDatabaseGivesNoWaypointsAndANotification() {
        val random = Random(1)
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(random.nextBytes(4096))
                }
            }
        }
        setNotFirstStartPreferences()
        launchActivity()
        waitUntilActivityVisible<WaypointsActivity>()
        app.migrateWaypoints()
        app.migrationIdlingResource.use {
            Espresso.onIdle()
        }
        assertNotDisplayed(R.id.waypointsRecyclerView)

        val notificationManager = app.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        Assert.assertTrue(
            "Event notification is displayed",
            notificationManager.activeNotifications.any {
                it.notification.extras.getString(Notification.EXTRA_TITLE) == "Error migrating waypoints" &&
                    it.notification.extras.getString(Notification.EXTRA_TEXT) == "An error occurred whilst migrating waypoints. Some may not have been migrated, so check the logs and re-add."
            }
        )
    }
}
