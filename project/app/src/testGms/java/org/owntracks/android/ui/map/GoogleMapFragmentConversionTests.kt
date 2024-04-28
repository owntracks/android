package org.owntracks.android.ui.map

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock

@RunWith(Parameterized::class)
class GoogleMapFragmentConversionTests(
    private val range1: ClosedRange<Double>,
    private val range2: ClosedRange<Double>,
    private val startingPoint: Double
) {
  @Test
  fun `given a starting point in a range, when running a linear conversion twice, then we should end up back in the same place`() {
    val fn = GoogleMapFragment(mock {}, mock {})::linearConversion
    assertEquals(
        startingPoint, fn(range1, range2, startingPoint).run { fn(range2, range1, this) }, 0.001)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "range1: {0} range2={1}, double={2})")
    fun data(): Iterable<Array<Any>> {
      val random = Random(1)
      return (1..500).map {
        val r1First = random.nextInt()
        val r1Second = random.nextInt()
        val r1 = if (r1First > r1Second) r1Second..r1First else r1First..r1Second

        val r2First = random.nextInt()
        val r2Second = random.nextInt()
        val r2 = if (r2First > r2Second) r2Second..r2First else r2First..r2Second

        val startingPoint = (random.nextInt(0, abs(r1First - r1Second)) + r1.first).toDouble()
        assertTrue(startingPoint >= r1.first && startingPoint <= r1.last)
        arrayOf(r1, r2, startingPoint)
      }
    }
  }
}
