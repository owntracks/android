package org.owntracks.android.preferences.types

import org.junit.Assert.assertEquals
import org.junit.Test

class StringMaxTwoAlphaNumericCharsTest {
    @Test
    fun `Given an input that is two alphanumeric characters when retrieving the valuethen the same string is returned`() {
        val s = StringMaxTwoAlphaNumericChars("ab")
        assertEquals("ab", s.toString())
    }
}
