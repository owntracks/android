package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MqttQos.MqttQosSerializer::class)
enum class MqttQos(val value: Int) {
  Zero(0),
  One(1),
  Two(2);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MqttQos = entries.firstOrNull { it.value == value } ?: One

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MqttQos =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: One
  }

  object MqttQosSerializer : KSerializer<MqttQos> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MqttQos", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MqttQos) {
      encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): MqttQos {
      val value = decoder.decodeInt()
      return entries.first { it.value == value }
    }
  }
}
