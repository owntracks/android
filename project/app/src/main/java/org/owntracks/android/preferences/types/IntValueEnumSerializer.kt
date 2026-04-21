package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Creates a [KSerializer] for an [Enum] that serializes as its integer [configValue].
 *
 * @param serialName the descriptor name (typically the enum class simple name)
 * @param entries all enum entries
 * @param configValue maps an entry to its integer wire value
 * @param default fallback used when the decoded integer doesn't match any entry
 */
internal fun <T : Enum<T>> intValueEnumSerializer(
    serialName: String,
    entries: List<T>,
    configValue: (T) -> Int,
    default: T,
): KSerializer<T> =
    object : KSerializer<T> {
      override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

      override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(configValue(value))

      override fun deserialize(decoder: Decoder): T {
        val v = decoder.decodeInt()
        return entries.firstOrNull { configValue(it) == v } ?: default
      }
    }
