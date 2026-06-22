package org.owntracks.android.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationProcessorTest {
  @Test
  fun `given disabled threshold, speed is not implausible`() {
    assertFalse(isImplausibleSpeed(100000.0, 1000, 0))
  }

  @Test
  fun `given plausible slow move, speed is not implausible`() {
    assertFalse(isImplausibleSpeed(100.0, 60000, 10))
  }

  @Test
  fun `given teleport move, speed is implausible`() {
    assertTrue(isImplausibleSpeed(24140.16, 5000, 500))
  }

  @Test
  fun `given zero time delta, speed is not implausible`() {
    assertFalse(isImplausibleSpeed(1000.0, 0, 10))
  }

  @Test
  fun `given negative time delta, speed is not implausible`() {
    assertFalse(isImplausibleSpeed(1000.0, -1000, 10))
  }
}
