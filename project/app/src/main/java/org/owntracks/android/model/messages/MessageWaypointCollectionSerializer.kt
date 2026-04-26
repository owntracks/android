package org.owntracks.android.model.messages

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.owntracks.android.support.MessageWaypointCollection

object MessageWaypointCollectionSerializer : KSerializer<MessageWaypointCollection> {
  private val listSerializer = ListSerializer(MessageWaypoint.serializer())

  override val descriptor: SerialDescriptor = listSerializer.descriptor

  override fun serialize(encoder: Encoder, value: MessageWaypointCollection) {
    val jsonEncoder = encoder as JsonEncoder
    val jsonArray =
        JsonArray(
            value.toList().map { waypoint ->
              buildJsonObject {
                put("_type", MessageWaypoint.TYPE)
                jsonEncoder.json
                    .encodeToJsonElement(MessageWaypoint.serializer(), waypoint)
                    .jsonObject
                    .forEach { (k, v) -> put(k, v) }
              }
            })
    jsonEncoder.encodeJsonElement(jsonArray)
  }

  override fun deserialize(decoder: Decoder): MessageWaypointCollection {
    return MessageWaypointCollection().apply {
      addAll(decoder.decodeSerializableValue(listSerializer))
    }
  }
}
