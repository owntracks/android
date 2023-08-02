package org.owntracks.android.data.waypoints

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions
import kotlin.random.Random
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class WaypointsMigrationFromObjectboxTestWithFullUI : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {

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
        app.migrateWaypoints()
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        BaristaDrawerInteractions.openDrawer()
        clickOn(R.string.title_activity_waypoints)
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
        app.migrateWaypoints()
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        BaristaDrawerInteractions.openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertDisplayed(R.id.waypointsRecyclerView)
        BaristaRecyclerViewAssertions.assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 1)
    }

    @Test
    fun migratingAnObjectboxWith10PointsProduces10Waypoints() {
        setNotFirstStartPreferences()
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/10-waypoints/data.mdb")!!.readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(dataBytes)
                }
            }
        }
        app.migrateWaypoints()
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        BaristaDrawerInteractions.openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertDisplayed(R.id.waypointsRecyclerView)
        BaristaRecyclerViewAssertions.assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 10)
    }

    @Test
    fun migratingAnObjectboxWith5000PointsProduces5000Waypoints() {
        setNotFirstStartPreferences()
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/5000-waypoints/data.mdb")!!.readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(dataBytes)
                }
            }
        }
        app.migrateWaypoints()
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        BaristaDrawerInteractions.openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertDisplayed(R.id.waypointsRecyclerView)
        BaristaRecyclerViewAssertions.assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 5000)
    }

    @Before
    fun clearNotifications() {
        // Cancel notifications
        (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        // Close the notification shade
        @Suppress("DEPRECATION") app.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    @Test
    fun migratingACorruptObjectboxDatabaseGivesNoWaypointsAndANotification() {
        setNotFirstStartPreferences()
        val random = Random(1)
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/").run {
            mkdirs()
            resolve("data.mdb").run {
                outputStream().use {
                    it.write(random.nextBytes(4096))
                }
            }
        }
        app.migrateWaypoints()
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        BaristaDrawerInteractions.openDrawer()
        clickOn(R.string.title_activity_waypoints)
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
