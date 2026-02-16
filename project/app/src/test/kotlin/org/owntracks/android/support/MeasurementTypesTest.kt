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

class MetersTest {
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
  fun `toFeet converts correctly`() {
    assertEquals(328.084, Meters(100).toFeet(), 0.001)
  }

  @Test
  fun `toMiles converts correctly`() {
    assertEquals(3.10686, Meters(5000).toMiles(), 0.001)
  }

  @Test
  fun `accuracy in metric returns meters`() {
    val result = Meters.formatAsAccuracy(mockContext, Meters(100), UnitsDisplay.METRIC)
    assertEquals("100 m", result)
  }

  @Test
  fun `accuracy in imperial returns feet`() {
    val result = Meters.formatAsAccuracy(mockContext, Meters(100), UnitsDisplay.IMPERIAL)
    assertEquals("328 ft", result)
  }

  @Test
  fun `altitude in metric returns meters`() {
    val result = Meters.formatAsAltitude(mockContext, Meters(500), UnitsDisplay.METRIC)
    assertEquals("500 m", result)
  }

  @Test
  fun `altitude in imperial returns feet`() {
    val result = Meters.formatAsAltitude(mockContext, Meters(500), UnitsDisplay.IMPERIAL)
    assertEquals("1640 ft", result)
  }

  @Test
  fun `short distance in metric returns meters`() {
    val result = Meters.formatAsDistance(mockContext, Meters(500f), UnitsDisplay.METRIC)
    assertEquals("500.0 m", result)
  }

  @Test
  fun `long distance in metric returns kilometres`() {
    val result = Meters.formatAsDistance(mockContext, Meters(5000f), UnitsDisplay.METRIC)
    assertEquals("5.0 km", result)
  }

  @Test
  fun `short distance in imperial returns feet`() {
    val result = Meters.formatAsDistance(mockContext, Meters(500f), UnitsDisplay.IMPERIAL)
    assertEquals("${Meters(500f).toFeet().toFloat()} ft", result)
  }

  @Test
  fun `long distance in imperial returns miles`() {
    val result = Meters.formatAsDistance(mockContext, Meters(5000f), UnitsDisplay.IMPERIAL)
    assertEquals("${Meters(5000f).toMiles().toFloat()} mi", result)
  }

  @Test
  fun `null units defaults to metric for accuracy`() {
    val result = Meters.formatAsAccuracy(mockContext, Meters(100), null)
    assertEquals("100 m", result)
  }

  @Test
  fun `int constructor works`() {
    assertEquals(100.0, Meters(100).value, 0.001)
  }

  @Test
  fun `float constructor works`() {
    assertEquals(100.5, Meters(100.5f).value, 0.001)
  }
}

class SpeedTest {
  private lateinit var mockContext: Context

  @Before
  fun setup() {
    mockContext = mock {
      on { getString(R.string.contactDetailsSpeedValue, 100) } doAnswer { "${it.arguments[1]} kph" }
      on { getString(R.string.contactDetailsSpeedValueImperial, 62) } doAnswer
          {
            "${it.arguments[1]} mph"
          }
    }
  }

  @Test
  fun `toMph converts correctly`() {
    assertEquals(62, Speed(100).toMph())
  }

  @Test
  fun `speed in metric returns kph`() {
    val result = Speed.format(mockContext, Speed(100), UnitsDisplay.METRIC)
    assertEquals("100 kph", result)
  }

  @Test
  fun `speed in imperial returns mph`() {
    val result = Speed.format(mockContext, Speed(100), UnitsDisplay.IMPERIAL)
    assertEquals("62 mph", result)
  }
}
