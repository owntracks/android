package org.owntracks.android.location

import androidx.core.location.LocationRequestCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class AospLocationProviderClientTest {

  @Test
  fun `Low power priorities map to low power quality so fused does not start GNSS`() {
    assertEquals(
        LocationRequestCompat.QUALITY_LOW_POWER,
        qualityForPriority(LocatorPriority.LowPower),
    )
    assertEquals(
        LocationRequestCompat.QUALITY_LOW_POWER,
        qualityForPriority(LocatorPriority.NoPower),
    )
  }

  @Test
  fun `Higher priorities keep their quality`() {
    assertEquals(
        LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY,
        qualityForPriority(LocatorPriority.BalancedPowerAccuracy),
    )
    assertEquals(
        LocationRequestCompat.QUALITY_HIGH_ACCURACY,
        qualityForPriority(LocatorPriority.HighAccuracy),
    )
  }
}
