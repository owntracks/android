package org.owntracks.android.data.waypoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class WaypointsMigrationFromObjectboxTest : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {
    @Test
    fun migratingAnEmptyObjectboxProducesZeroWaypoints() {
        setNotFirstStartPreferences()
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/empty/data.mdb")!!
            .readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/")
            .run {
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
        openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertNotDisplayed(R.id.waypointsRecyclerView)
    }

    @Test
    fun migratingAnObjectboxWithSinglePointProducesOneWaypoint() {
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/single-waypoint/data.mdb")!!
            .readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/")
            .run {
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
        openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertDisplayed(R.id.waypointsRecyclerView)
        assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 1)
    }

    @Test
    fun migratingAnObjectboxWith10PointsProduces50Waypoints() {
        setNotFirstStartPreferences()
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/10-waypoints/data.mdb")!!
            .readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/")
            .run {
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
        openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertDisplayed(R.id.waypointsRecyclerView)
        assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 10)
    }

    @Test
    fun migratingAnObjectboxWith5000PointsProduces5000Waypoints() {
        setNotFirstStartPreferences()
        val dataBytes = this.javaClass.getResource("/objectbox-lmdbs/5000-waypoints/data.mdb")!!
            .readBytes()
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir.resolve("objectbox/objectbox/")
            .run {
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
        openDrawer()
        clickOn(R.string.title_activity_waypoints)
        assertDisplayed(R.id.waypointsRecyclerView)
        assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 5000)
    }
}
