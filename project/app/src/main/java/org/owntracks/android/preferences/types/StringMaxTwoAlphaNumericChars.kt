package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class StringMaxTwoAlphaNumericChars private constructor(@JsonValue val value: String) {
  init {
    require(value.length <= 2) { "Length much be two characters or fewer" }
  }

  override fun toString(): String {
    return value
  }

  companion object {
    operator fun invoke(value: String): StringMaxTwoAlphaNumericChars {
      return StringMaxTwoAlphaNumericChars(value.filter { Character.isLetterOrDigit(it) }.take(2))
    }
  }
}
