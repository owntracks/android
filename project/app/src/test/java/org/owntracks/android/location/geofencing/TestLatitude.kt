package org.owntracks.android.location.geofencing

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TestLatitude(private val parameter: Parameter) {
  @Test
  fun `Latitude can parse an input double into the appropriate range`() {
    val latitude = Latitude(parameter.input)
    assertEquals(parameter.expected, latitude.value, 0.0001)
  }

  data class Parameter(val input: Double, val expected: Double)

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0} (input={1}, expected={2})")
    fun data(): Iterable<Parameter> {
      return arrayListOf(
          Parameter(0.0, 0.0),
          Parameter(45.0, 45.0),
          Parameter(90.0, 90.0),
          Parameter(135.0, 45.0),
          Parameter(180.0, 0.0),
          Parameter(225.0, -45.0),
          Parameter(270.0, -90.0),
          Parameter(315.0, -45.0),
          Parameter(360.0, 0.0),
          Parameter(405.0, 45.0),
          Parameter(450.0, 90.0),
          Parameter(495.0, 45.0),
          Parameter(540.0, 0.0),
          Parameter(585.0, -45.0),
          Parameter(630.0, -90.0),
          Parameter(675.0, -45.0),
          Parameter(720.0, 0.0),
          Parameter(-45.0, -45.0),
          Parameter(-90.0, -90.0),
          Parameter(-135.0, -45.0),
          Parameter(-180.0, 0.0),
          Parameter(-225.0, 45.0),
          Parameter(-270.0, 90.0),
          Parameter(-315.0, 45.0),
          Parameter(-360.0, 0.0),
          Parameter(-405.0, -45.0),
          Parameter(-450.0, -90.0),
          Parameter(-495.0, -45.0),
          Parameter(-540.0, 0.0),
          Parameter(-585.0, 45.0),
          Parameter(-630.0, 90.0),
          Parameter(-675.0, 45.0),
          Parameter(-720.0, 0.0))
    }
  }
}
