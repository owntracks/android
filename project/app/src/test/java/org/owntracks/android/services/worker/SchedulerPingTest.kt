package org.owntracks.android.services.worker

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.location.profiles.LocatorSettings
import org.owntracks.android.location.profiles.StaticLocation
import org.owntracks.android.preferences.types.MonitoringMode

/**
 * Periodic work runs once as soon as it's scheduled. That's the ping being sent on start, except
 * when a static location has just been sent on arriving at it, where it would be a duplicate.
 */
class SchedulerPingTest {
  private fun settings(ping: Int, staticLocation: StaticLocation? = null) =
      LocatorSettings(MonitoringMode.Significant, 60, 10, 500, null, ping, staticLocation)

  @Test
  fun `the ping is sent straight away, then every interval`() {
    assertEquals(
        Scheduler.PingSchedule(15.minutes, Duration.ZERO),
        Scheduler.pingScheduleFor(settings(15)),
    )
  }

  @Test
  fun `a static location's first ping waits an interval, as it's just been sent`() {
    assertEquals(
        Scheduler.PingSchedule(30.minutes, 30.minutes),
        Scheduler.pingScheduleFor(settings(30, StaticLocation(1.0, 2.0))),
    )
  }
}
