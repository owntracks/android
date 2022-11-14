package org.owntracks.android.preferences.types

data class StringMaxTwoAlphaNumericChars(private val input: String) {
    val value: String

    init {
        value = input.filter { Character.isLetterOrDigit(it) }.toString().substring(0, 2)
    }

    override fun toString(): String {
        return value
    }
}
