package org.owntracks.android.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.owntracks.android.location.LatLng

class FitTargetTest {
  @Test
  fun `nothing to show gives no target`() {
    assertNull(FitTarget.of(emptyList()))
  }

  @Test
  fun `a single point is centered on`() {
    assertEquals(FitTarget.Point(LatLng(51.5, -0.1)), FitTarget.of(listOf(LatLng(51.5, -0.1))))
  }

  @Test
  fun `points practically on top of each other are centered on rather than framed`() {
    val target = FitTarget.of(listOf(LatLng(51.5, -0.1), LatLng(51.5001, -0.1001)))
    assertEquals(FitTarget.Point(LatLng(51.50005, -0.10005)), target)
  }

  @Test
  fun `spread out points are framed by their bounding box`() {
    val target = FitTarget.of(listOf(LatLng(51.5, -0.1), LatLng(48.85, 2.35), LatLng(52.52, 13.4)))
    assertEquals(FitTarget.Bounds(LatLng(48.85, -0.1), LatLng(52.52, 13.4)), target)
  }

  @Test
  fun `points close in latitude but apart in longitude are still framed`() {
    val target = FitTarget.of(listOf(LatLng(51.5, -0.1), LatLng(51.5, 0.1)))
    assertEquals(FitTarget.Bounds(LatLng(51.5, -0.1), LatLng(51.5, 0.1)), target)
  }
}
