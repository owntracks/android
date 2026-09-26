package org.owntracks.android.services

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.owntracks.android.location.geofencing.Geofence

class LocationProcessorTransitionDebounceTest {
  private val dwell: Duration = Duration.ofMinutes(2)
  private val t0: Instant = Instant.parse("2026-07-20T10:00:00Z")

  @Test
  fun `first candidate observed starts the timer without committing`() {
    val (transitionToCommit, pending) =
        resolveTransitionDebounce(null, Geofence.GEOFENCE_TRANSITION_EXIT, t0, dwell)

    assertNull(transitionToCommit)
    assertEquals(PendingWaypointTransition(Geofence.GEOFENCE_TRANSITION_EXIT, t0), pending)
  }

  @Test
  fun `same candidate before dwell elapses does not commit`() {
    val pendingSince = PendingWaypointTransition(Geofence.GEOFENCE_TRANSITION_EXIT, t0)
    val now = t0.plus(dwell.minusSeconds(1))

    val (transitionToCommit, pending) =
        resolveTransitionDebounce(pendingSince, Geofence.GEOFENCE_TRANSITION_EXIT, now, dwell)

    assertNull(transitionToCommit)
    assertEquals(pendingSince, pending)
  }

  @Test
  fun `same candidate once dwell elapses commits and clears pending`() {
    val pendingSince = PendingWaypointTransition(Geofence.GEOFENCE_TRANSITION_EXIT, t0)
    val now = t0.plus(dwell)

    val (transitionToCommit, pending) =
        resolveTransitionDebounce(pendingSince, Geofence.GEOFENCE_TRANSITION_EXIT, now, dwell)

    assertEquals(Geofence.GEOFENCE_TRANSITION_EXIT, transitionToCommit)
    assertNull(pending)
  }

  @Test
  fun `candidate flip before commit resets the timer to the new candidate`() {
    val pendingSince = PendingWaypointTransition(Geofence.GEOFENCE_TRANSITION_EXIT, t0)
    val now = t0.plusSeconds(90)

    val (transitionToCommit, pending) =
        resolveTransitionDebounce(pendingSince, Geofence.GEOFENCE_TRANSITION_ENTER, now, dwell)

    assertNull(transitionToCommit)
    assertEquals(PendingWaypointTransition(Geofence.GEOFENCE_TRANSITION_ENTER, now), pending)
  }
}
