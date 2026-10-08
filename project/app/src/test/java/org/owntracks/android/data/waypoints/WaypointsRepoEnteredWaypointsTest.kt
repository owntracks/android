package org.owntracks.android.data.waypoints

import android.content.Context
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.location.geofencing.Geofence

class WaypointsRepoEnteredWaypointsTest {
  private fun waypoint(tst: Long, transition: Int = 0) =
      WaypointModel(tst = Instant.ofEpochSecond(tst), lastTransition = transition)

  @Test
  fun `inserting an entered waypoint adds it to the entered waypoints`() = runTest {
    val repo = InMemoryWaypointsRepo(this, mock<Context>(), Dispatchers.Unconfined)
    repo.insert(waypoint(100, Geofence.GEOFENCE_TRANSITION_ENTER))
    assertEquals(setOf(100L), repo.enteredWaypointTsts.value)
  }

  @Test
  fun `a waypoint that hasn't been entered is not an entered waypoint`() = runTest {
    val repo = InMemoryWaypointsRepo(this, mock<Context>(), Dispatchers.Unconfined)
    repo.insert(waypoint(100))
    assertEquals(emptySet<Long>(), repo.enteredWaypointTsts.value)
  }

  @Test
  fun `exiting a waypoint without notifying still removes it from the entered waypoints`() =
      runTest {
        val repo = InMemoryWaypointsRepo(this, mock<Context>(), Dispatchers.Unconfined)
        val waypoint = waypoint(100, Geofence.GEOFENCE_TRANSITION_ENTER)
        repo.insert(waypoint)
        waypoint.lastTransition = Geofence.GEOFENCE_TRANSITION_EXIT
        repo.update(waypoint, false)
        assertEquals(emptySet<Long>(), repo.enteredWaypointTsts.value)
      }

  @Test
  fun `entering one waypoint doesn't affect another that's already entered`() = runTest {
    val repo = InMemoryWaypointsRepo(this, mock<Context>(), Dispatchers.Unconfined)
    repo.insert(waypoint(100, Geofence.GEOFENCE_TRANSITION_ENTER))
    val other = waypoint(200)
    repo.insert(other)
    other.lastTransition = Geofence.GEOFENCE_TRANSITION_ENTER
    repo.update(other, false)
    assertEquals(setOf(100L, 200L), repo.enteredWaypointTsts.value)
  }

  @Test
  fun `deleting an entered waypoint removes it from the entered waypoints`() = runTest {
    val repo = InMemoryWaypointsRepo(this, mock<Context>(), Dispatchers.Unconfined)
    val waypoint = waypoint(100, Geofence.GEOFENCE_TRANSITION_ENTER)
    repo.insert(waypoint)
    repo.delete(waypoint)
    assertEquals(emptySet<Long>(), repo.enteredWaypointTsts.value)
  }

  @Test
  fun `clearing the waypoints empties the entered waypoints`() = runTest {
    val repo = InMemoryWaypointsRepo(this, mock<Context>(), Dispatchers.Unconfined)
    repo.insert(waypoint(100, Geofence.GEOFENCE_TRANSITION_ENTER))
    repo.clearAll()
    assertEquals(emptySet<Long>(), repo.enteredWaypointTsts.value)
  }
}
