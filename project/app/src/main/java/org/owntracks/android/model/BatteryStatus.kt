package org.owntracks.android.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BatteryStatus.BatteryStatusSerializer::class)
enum class BatteryStatus(val value: Int) {
  /** The owntracks model for battery status */
  UNKNOWN(0),
  UNPLUGGED(1),
  CHARGING(2),
  FULL(3);

  object BatteryStatusSerializer : KSerializer<BatteryStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BatteryStatus", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: BatteryStatus) {
      encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): BatteryStatus {
      val value = decoder.decodeInt()
      return entries.first { it.value == value }
    }
  }
}
