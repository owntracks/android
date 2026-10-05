package org.owntracks.android.location

import org.junit.Assert.assertEquals
import org.junit.Test

class LocatorPriorityTest {

  @Test
  fun `Resolving by integer value matches the configuration values`() {
    assertEquals(LocatorPriority.NoPower, LocatorPriority.getByValue(0))
    assertEquals(LocatorPriority.LowPower, LocatorPriority.getByValue(1))
    assertEquals(LocatorPriority.BalancedPowerAccuracy, LocatorPriority.getByValue(2))
    assertEquals(LocatorPriority.HighAccuracy, LocatorPriority.getByValue(3))
  }

  @Test
  fun `Resolving by numeric string matches the configuration values`() {
    assertEquals(LocatorPriority.NoPower, LocatorPriority.getByValue("0"))
    assertEquals(LocatorPriority.HighAccuracy, LocatorPriority.getByValue("3"))
  }

  @Test
  fun `Resolving an unknown value gets balanced power accuracy`() {
    assertEquals(LocatorPriority.BalancedPowerAccuracy, LocatorPriority.getByValue(7))
    assertEquals(LocatorPriority.BalancedPowerAccuracy, LocatorPriority.getByValue("nope"))
  }
}
