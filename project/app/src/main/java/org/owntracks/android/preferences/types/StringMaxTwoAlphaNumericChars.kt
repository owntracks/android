package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = StringMaxTwoAlphaNumericChars.StringMaxTwoAlphaNumericCharsSerializer::class)
@JvmInline
value class StringMaxTwoAlphaNumericChars private constructor(val value: String) {
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

  object StringMaxTwoAlphaNumericCharsSerializer : KSerializer<StringMaxTwoAlphaNumericChars> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringMaxTwoAlphaNumericChars", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StringMaxTwoAlphaNumericChars) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): StringMaxTwoAlphaNumericChars {
      return StringMaxTwoAlphaNumericChars(decoder.decodeString())
    }
  }
}
