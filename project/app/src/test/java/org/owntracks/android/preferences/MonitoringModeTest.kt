package org.owntracks.android.preferences

import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.preferences.types.MonitoringMode

class MonitoringModeTest {

  @Test
  fun `Resolving quiet mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.QUIET, MonitoringMode.getByValue(-1))
  }

  @Test
  fun `Resolving manual mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.MANUAL, MonitoringMode.getByValue(0))
  }

  @Test
  fun `Resolving significant mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.SIGNIFICANT, MonitoringMode.getByValue(1))
  }

  @Test
  fun `Resolving move mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.MOVE, MonitoringMode.getByValue(2))
  }

  @Test
  fun `Resolving unknown value gets default significant mode`() {
    assertEquals(MonitoringMode.SIGNIFICANT, MonitoringMode.getByValue(50))
  }

  @Test
  fun `The next mode after quiet is manual`() {
    assertEquals(MonitoringMode.MANUAL, MonitoringMode.QUIET.next())
  }

  @Test
  fun `The next mode after manual is significant`() {
    assertEquals(MonitoringMode.SIGNIFICANT, MonitoringMode.MANUAL.next())
  }

  @Test
  fun `The next mode after significant is move`() {
    assertEquals(MonitoringMode.MOVE, MonitoringMode.SIGNIFICANT.next())
  }

  @Test
  fun `The next mode after move is quiet`() {
    assertEquals(MonitoringMode.QUIET, MonitoringMode.MOVE.next())
  }
}
