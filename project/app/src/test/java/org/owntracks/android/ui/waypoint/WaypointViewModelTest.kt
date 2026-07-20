package org.owntracks.android.ui.waypoint

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.waypoints.InMemoryWaypointsRepo
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude

/** Regression tests for https://github.com/owntracks/android/issues/2130. */
@OptIn(ExperimentalCoroutinesApi::class)
class WaypointViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mock()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `loading an existing waypoint never exposes the current device location as its coordinates`() =
      runTest(testDispatcher) {
        val existingWaypoint =
            WaypointModel(
                id = 42,
                description = "Home",
                geofenceLatitude = Latitude(51.5),
                geofenceLongitude = Longitude(-0.1),
                geofenceRadius = 100)
        val waypointsRepo = InMemoryWaypointsRepo(this, mockContext, testDispatcher)
        waypointsRepo.insert(existingWaypoint)
        val locationRepo =
            LocationRepo().apply { currentBlueDotOnMapLocation = LatLng(1.23, 4.56) }

        val viewModel = WaypointViewModel(waypointsRepo, locationRepo)
        viewModel.loadWaypoint(existingWaypoint.id)

        // Before the async load from the repo has resolved, the ViewModel must not expose the
        // device's current location as the waypoint's coordinates, and must flag that it's not
        // ready to be edited/saved yet.
        assertTrue(viewModel.isLoading.value)
        assertEquals(existingWaypoint.id, viewModel.waypoint.value.id)
        assertEquals(0.0, viewModel.waypoint.value.geofenceLatitude.value, 0.0)
        assertEquals(0.0, viewModel.waypoint.value.geofenceLongitude.value, 0.0)

        advanceUntilIdle()

        // Once the load resolves, the real waypoint is shown and editing/saving is unblocked.
        assertFalse(viewModel.isLoading.value)
        assertEquals(existingWaypoint, viewModel.waypoint.value)
      }

  @Test
  fun `creating a new waypoint shows the current device location immediately`() = runTest {
    val waypointsRepo = InMemoryWaypointsRepo(this, mockContext, testDispatcher)
    val locationRepo = LocationRepo().apply { currentBlueDotOnMapLocation = LatLng(1.23, 4.56) }

    val viewModel = WaypointViewModel(waypointsRepo, locationRepo)

    assertFalse(viewModel.isLoading.value)
    assertEquals(Latitude(1.23), viewModel.waypoint.value.geofenceLatitude)
    assertEquals(Longitude(4.56), viewModel.waypoint.value.geofenceLongitude)
  }
}
