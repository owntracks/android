package org.owntracks.android.location.profiles

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextProfileEditingTest {
  private val a = ContextProfile("a", "A", emptyList(), LocatorOverrides())
  private val b = ContextProfile("b", "B", emptyList(), LocatorOverrides())
  private val c = ContextProfile("c", "C", emptyList(), LocatorOverrides())

  @Test
  fun `replacing a profile keeps its position`() {
    val renamed = b.copy(name = "Bee")
    assertEquals(listOf(a, renamed, c), listOf(a, b, c).replacing(renamed))
  }

  @Test
  fun `replacing a profile that isn't there adds it to the end`() {
    assertEquals(listOf(a, b, c), listOf(a, b).replacing(c))
  }

  @Test
  fun `a profile can be removed`() {
    assertEquals(listOf(a, c), listOf(a, b, c).without("b"))
  }

  @Test
  fun `removing a profile that isn't there changes nothing`() {
    assertEquals(listOf(a, b), listOf(a, b).without("z"))
  }

  @Test
  fun `a profile can be moved up`() {
    assertEquals(listOf(a, c, b), listOf(a, b, c).moving("c", -1))
  }

  @Test
  fun `a profile can be moved down`() {
    assertEquals(listOf(b, a, c), listOf(a, b, c).moving("a", 1))
  }

  @Test
  fun `moving past either end leaves the profile at that end`() {
    assertEquals(listOf(a, b, c), listOf(a, b, c).moving("a", -1))
    assertEquals(listOf(a, b, c), listOf(a, b, c).moving("c", 5))
  }

  @Test
  fun `moving a profile that isn't there changes nothing`() {
    assertEquals(listOf(a, b, c), listOf(a, b, c).moving("z", 1))
  }
}
