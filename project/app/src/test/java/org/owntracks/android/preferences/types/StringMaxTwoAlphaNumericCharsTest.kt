package org.owntracks.android.preferences.types

import org.junit.Assert.assertEquals
import org.junit.Test

class StringMaxTwoAlphaNumericCharsTest {
  @Test
  fun `Given an input that is two alphanumeric characters when retrieving the valuethen the same string is returned`() {
    val s = StringMaxTwoAlphaNumericChars("ab")
    assertEquals("ab", s.toString())
  }

  @Test
  fun `Given an input that is more than two characters when retrieving the value then the truncated string is returned`() {
    val s = StringMaxTwoAlphaNumericChars("abcde")
    assertEquals("ab", s.toString())
  }

  @Test
  fun `Given an input that contains non-alphanumeric characters when retrieving the value then the returned string has these removed`() {
    val s = StringMaxTwoAlphaNumericChars("a b")
    assertEquals("ab", s.toString())
  }

  @Test
  fun `Given an input that contains only one character when retrieving the value then the same string is returned`() {
    val s = StringMaxTwoAlphaNumericChars("a")
    assertEquals("a", s.toString())
  }

  @Test
  fun `Given an empty input when retrieving the value then an empty string is returned`() {
    val s = StringMaxTwoAlphaNumericChars("")
    assertEquals("", s.toString())
  }
}
