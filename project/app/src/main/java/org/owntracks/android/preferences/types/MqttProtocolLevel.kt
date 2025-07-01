package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MqttProtocolLevel.MqttProtocolLevelSerializer::class)
enum class MqttProtocolLevel(val value: Int) {
  MQTT_3_1(3),
  MQTT_3_1_1(4);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MqttProtocolLevel =
        entries.firstOrNull { it.value == value } ?: MQTT_3_1

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MqttProtocolLevel =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: MQTT_3_1
  }

  object MqttProtocolLevelSerializer : KSerializer<MqttProtocolLevel> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MqttProtocolLevel", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MqttProtocolLevel) {
      encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): MqttProtocolLevel {
      val value = decoder.decodeInt()
      return entries.first { it.value == value }
    }
  }
}
