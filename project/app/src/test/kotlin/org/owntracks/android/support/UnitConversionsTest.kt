package org.owntracks.android.support

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.owntracks.android.R
import org.owntracks.android.preferences.types.UnitsDisplay

class UnitConversionsTest {
  private lateinit var mockContext: Context

  @Before
  fun setup() {
    mockContext = mock {
      on { getString(R.string.contactDetailsAccuracyValue, 100) } doAnswer
          {
            "${it.arguments[1]} m"
          }
      on { getString(R.string.contactDetailsAccuracyValueImperial, 328) } doAnswer
          {
            "${it.arguments[1]} ft"
          }
      on { getString(R.string.contactDetailsAltitudeValue, 500) } doAnswer
          {
            "${it.arguments[1]} m"
          }
      on { getString(R.string.contactDetailsAltitudeValueImperial, 1640) } doAnswer
          {
            "${it.arguments[1]} ft"
          }
      on { getString(R.string.contactDetailsSpeedValue, 100) } doAnswer { "${it.arguments[1]} kph" }
      on { getString(R.string.contactDetailsSpeedValueImperial, 62) } doAnswer
          {
            "${it.arguments[1]} mph"
          }
      on { getString(R.string.contactDetailsDistanceUnitMeters) } doAnswer { "m" }
      on { getString(R.string.contactDetailsDistanceUnitKilometres) } doAnswer { "km" }
      on { getString(R.string.contactDetailsDistanceUnitFeet) } doAnswer { "ft" }
      on { getString(R.string.contactDetailsDistanceUnitMiles) } doAnswer { "mi" }
      on { getString(any<Int>(), any(), any()) } doAnswer
          {
            "${it.arguments[1]} ${it.arguments[2]}"
          }
    }
  }

  @Test
  fun `accuracy in metric returns meters`() {
    val result = UnitConversions.formatAccuracy(mockContext, 100, UnitsDisplay.METRIC)
    assertEquals("100 m", result)
  }

  @Test
  fun `accuracy in imperial returns feet`() {
    val result = UnitConversions.formatAccuracy(mockContext, 100, UnitsDisplay.IMPERIAL)
    assertEquals("328 ft", result)
  }

  @Test
  fun `altitude in metric returns meters`() {
    val result = UnitConversions.formatAltitude(mockContext, 500, UnitsDisplay.METRIC)
    assertEquals("500 m", result)
  }

  @Test
  fun `altitude in imperial returns feet`() {
    val result = UnitConversions.formatAltitude(mockContext, 500, UnitsDisplay.IMPERIAL)
    assertEquals("1640 ft", result)
  }

  @Test
  fun `speed in metric returns kph`() {
    val result = UnitConversions.formatSpeed(mockContext, 100, UnitsDisplay.METRIC)
    assertEquals("100 kph", result)
  }

  @Test
  fun `speed in imperial returns mph`() {
    val result = UnitConversions.formatSpeed(mockContext, 100, UnitsDisplay.IMPERIAL)
    assertEquals("62 mph", result)
  }

  @Test
  fun `short distance in metric returns meters`() {
    val result = UnitConversions.formatDistance(mockContext, 500f, UnitsDisplay.METRIC)
    assertEquals("500.0 m", result)
  }

  @Test
  fun `long distance in metric returns kilometres`() {
    val result = UnitConversions.formatDistance(mockContext, 5000f, UnitsDisplay.METRIC)
    assertEquals("5.0 km", result)
  }

  @Test
  fun `short distance in imperial returns feet`() {
    val result = UnitConversions.formatDistance(mockContext, 500f, UnitsDisplay.IMPERIAL)
    assertEquals("${500f * 3.28084f} ft", result)
  }

  @Test
  fun `long distance in imperial returns miles`() {
    val result = UnitConversions.formatDistance(mockContext, 5000f, UnitsDisplay.IMPERIAL)
    assertEquals("${5000f / 1609.34f} mi", result)
  }

  @Test
  fun `null units defaults to metric for accuracy`() {
    val result = UnitConversions.formatAccuracy(mockContext, 100, null)
    assertEquals("100 m", result)
  }
}
