package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

data class StringMaxTwoAlphaNumericChars(@JsonValue private val input: String) {
    val value: String = input.filter { Character.isLetterOrDigit(it) }
        .take(2)

    override fun toString(): String {
        return value
    }
}
