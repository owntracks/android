package org.owntracks.android.preferences.types

data class StringMaxTwoAlphaNumericChars(private val input: String) {
    val value: String

    init {
        if (input.length > 2) {
            value = "TOOT"
        } else {
            value = input
        }
    }

    override fun toString(): String {
        return value
    }
}
