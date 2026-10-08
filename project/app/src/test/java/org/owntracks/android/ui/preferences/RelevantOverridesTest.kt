package org.owntracks.android.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.location.profiles.LocatorOverrides
import org.owntracks.android.location.profiles.StaticLocation
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.ui.preferences.LocatorOverride.LocatorDisplacement
import org.owntracks.android.ui.preferences.LocatorOverride.LocatorInterval
import org.owntracks.android.ui.preferences.LocatorOverride.LocatorPriority
import org.owntracks.android.ui.preferences.LocatorOverride.MoveModeLocatorInterval
import org.owntracks.android.ui.preferences.LocatorOverride.Ping

class RelevantOverridesTest {
  @Test
  fun `without a monitoring mode every override could apply`() {
    assertEquals(LocatorOverride.entries.toSet(), relevantOverrides(LocatorOverrides()))
  }

  @Test
  fun `quiet and manual still locate, but don't ping`() {
    listOf(MonitoringMode.Quiet, MonitoringMode.Manual).forEach {
      assertEquals(
          setOf(LocatorPriority, LocatorInterval, LocatorDisplacement),
          relevantOverrides(LocatorOverrides(monitoring = it)),
      )
    }
  }

  @Test
  fun `significant uses the normal interval and displacement`() {
    assertEquals(
        setOf(LocatorPriority, LocatorInterval, LocatorDisplacement, Ping),
        relevantOverrides(LocatorOverrides(monitoring = MonitoringMode.Significant)),
    )
  }

  @Test
  fun `move uses the move interval and no displacement`() {
    assertEquals(
        setOf(LocatorPriority, MoveModeLocatorInterval, Ping),
        relevantOverrides(LocatorOverrides(monitoring = MonitoringMode.Move)),
    )
  }

  @Test
  fun `a static location only needs the ping`() {
    assertEquals(
        setOf(Ping),
        relevantOverrides(LocatorOverrides(staticLocation = StaticLocation(1.0, 2.0))),
    )
  }

  @Test
  fun `a static location in quiet mode needs nothing`() {
    assertEquals(
        emptySet<LocatorOverride>(),
        relevantOverrides(
            LocatorOverrides(
                monitoring = MonitoringMode.Quiet,
                staticLocation = StaticLocation(1.0, 2.0),
            )
        ),
    )
  }
}
