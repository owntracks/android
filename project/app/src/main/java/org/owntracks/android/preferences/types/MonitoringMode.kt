package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MonitoringMode.MonitoringModeSerializer::class)
enum class MonitoringMode(val value: Int) {
  Quiet(-1),
  Manual(0),
  Significant(1),
  Move(2);

  fun next(): MonitoringMode =
      when (this) {
        Quiet -> Manual
        Manual -> Significant
        Significant -> Move
        Move -> Quiet
      }

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MonitoringMode =
        entries.firstOrNull { it.value == value } ?: Significant

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MonitoringMode =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: Significant
  }

  object MonitoringModeSerializer : KSerializer<MonitoringMode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MonitoringMode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MonitoringMode) {
      encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): MonitoringMode {
      val value = decoder.decodeInt()
      return entries.first { it.value == value }
    }
  }
}
