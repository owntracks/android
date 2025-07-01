package org.owntracks.android.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = CommandAction.CommandActionSerializer::class)
enum class CommandAction(val value: String) {
  /** The owntracks model for command actions */
  REPORT_LOCATION("reportLocation"),
  SET_WAYPOINTS("setWaypoints"),
  CLEAR_WAYPOINTS("clearWaypoints"),
  SET_CONFIGURATION("setConfiguration"),
  WAYPOINTS("waypoints"),
  STATUS("status");

  object CommandActionSerializer : KSerializer<CommandAction> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("CommandAction", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CommandAction) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): CommandAction {
      val value = decoder.decodeString()
      return entries.first { it.value == value }
    }
  }
}
