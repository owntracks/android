package org.owntracks.android.waypoints

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.owntracks.android.data.waypoints.RoomWaypointsRepo
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.testutils.use

@SmallTest
@RunWith(Parameterized::class)
@HiltAndroidTest
@Ignore("Needs git LFS for the moment, which doesn't work on CI")
class WaypointsMigrationFromObjectboxTest(private val parameter: Parameter) {
  @Before
  fun clearLocalPackageData() {
    InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("waypoints")
  }

  @Test
  fun migratingAnObjectboxProducesCorrectNumberOfWaypoints() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dataBytes =
        this.javaClass.getResource("/objectbox-lmdbs/${parameter.dbName}/data.mdb")!!.readBytes()
    context.filesDir.resolve("objectbox/objectbox/").run {
      mkdirs()
      resolve("data.mdb").run { outputStream().use { it.write(dataBytes) } }
    }

    val migrationIdlingResource = SimpleIdlingResource("waypointsMigrationIdlingResource", false)
    val roomWaypointsRepo =
        RoomWaypointsRepo(
            context,
            Dispatchers.IO.limitedParallelism(1),
            CoroutineScope(SupervisorJob()),
            migrationIdlingResource,
        )
    migrationIdlingResource.use { Espresso.onIdle() }
    runBlocking {
      assertEquals(
          parameter.expectedCount,
          roomWaypointsRepo.getAll().size,
      )
    }
  }

  data class Parameter(val dbName: String, val expectedCount: Int)

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "(dbname={1},expected={2})")
    fun data(): Iterable<Parameter> {
      return arrayListOf(
          Parameter("single-waypoint", 1),
          Parameter("10-waypoints", 10),
          Parameter("5000-waypoints", 5000),
          Parameter("empty", 0),
          Parameter("3-created-by-real-device", 3))
    }
  }
}
