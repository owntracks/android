package org.owntracks.android.preferences

import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.preferences.types.MonitoringMode

class MonitoringModeTest {

  @Test
  fun `Resolving quiet mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.Quiet, MonitoringMode.getByValue(-1))
  }

  @Test
  fun `Resolving manual mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.Manual, MonitoringMode.getByValue(0))
  }

  @Test
  fun `Resolving significant mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.Significant, MonitoringMode.getByValue(1))
  }

  @Test
  fun `Resolving move mode by value gets quiet mode`() {
    assertEquals(MonitoringMode.Move, MonitoringMode.getByValue(2))
  }

  @Test
  fun `Resolving unknown value gets default significant mode`() {
    assertEquals(MonitoringMode.Significant, MonitoringMode.getByValue(50))
  }

  @Test
  fun `The next mode after quiet is manual`() {
    assertEquals(MonitoringMode.Manual, MonitoringMode.Quiet.next())
  }

  @Test
  fun `The next mode after manual is significant`() {
    assertEquals(MonitoringMode.Significant, MonitoringMode.Manual.next())
  }

  @Test
  fun `The next mode after significant is move`() {
    assertEquals(MonitoringMode.Move, MonitoringMode.Significant.next())
  }

  @Test
  fun `The next mode after move is quiet`() {
    assertEquals(MonitoringMode.Quiet, MonitoringMode.Move.next())
  }
}
