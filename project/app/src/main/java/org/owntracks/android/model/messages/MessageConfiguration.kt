package org.owntracks.android.model.messages

import android.annotation.SuppressLint
import kotlin.reflect.full.createType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.owntracks.android.support.MessageWaypointCollection
import timber.log.Timber

@SuppressLint("UnsafeOptInUsageError")
@Serializable(with = MessageConfiguration.MessageConfigurationSerializer::class)
@SerialName(MessageConfiguration.TYPE)
class MessageConfiguration(
    @Transient private val messageWithId: MessageWithId = MessageWithRandomId()
) : MessageBase(), MessageWithId {
  private val map: MutableMap<String, Any?> = mutableMapOf()

  @Transient override var messageId: MessageId = messageWithId.messageId

  var waypoints: MessageWaypointCollection = MessageWaypointCollection()

  fun any(): Map<String, Any?> {
    return map
  }

  operator fun set(key: String, value: Any?) {
    map[key] = value
  }

  operator fun get(key: String?): Any? {
    return map[key]
  }

  fun containsKey(key: String?): Boolean {
    return map.containsKey(key)
  }

  val keys: Set<String>
    get() = map.keys

  companion object {
    const val TYPE = "configuration"
  }

  override fun toString(): String = "[MessageConfiguration]"

  object MessageConfigurationSerializer : KSerializer<MessageConfiguration> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("configuration")

    override fun serialize(encoder: Encoder, value: MessageConfiguration) {
      val jsonEncoder = encoder as JsonEncoder
      val obj = buildJsonObject {
        put("_type", TYPE)
        put("_id", value.messageId)
        value.map.forEach { (k, v) ->
          when (v) {
            is Boolean -> put(k, JsonPrimitive(v))
            is Int -> put(k, JsonPrimitive(v))
            is Long -> put(k, JsonPrimitive(v))
            is Float -> put(k, JsonPrimitive(v))
            is Double -> put(k, JsonPrimitive(v))
            is String -> put(k, JsonPrimitive(v))
            is Enum<*> -> {
              @Suppress("UNCHECKED_CAST")
              put(
                  k,
                  jsonEncoder.json.encodeToJsonElement(
                      serializer(v::class.createType()) as KSerializer<Any>, v))
            }
            is Set<*> -> put(k, JsonArray(v.map { JsonPrimitive(it?.toString() ?: "") }))
            null -> put(k, JsonNull)
            else -> put(k, JsonPrimitive(v.toString()))
          }
        }
        put(
            "waypoints",
            JsonArray(
                value.waypoints.map { waypoint ->
                  buildJsonObject {
                    put("_type", MessageWaypoint.TYPE)
                    jsonEncoder.json
                        .encodeToJsonElement(MessageWaypoint.serializer(), waypoint)
                        .jsonObject
                        .forEach { (k, v) -> put(k, v) }
                  }
                }))
      }
      jsonEncoder.encodeJsonElement(obj)
    }

    override fun deserialize(decoder: Decoder): MessageConfiguration {
      val jsonDecoder = decoder as JsonDecoder
      val obj = jsonDecoder.decodeJsonElement() as JsonObject
      return MessageConfiguration().apply {
        obj.forEach { (key, element) ->
          when (key) {
            "_type" -> {}
            "_id" -> messageId = (element as? JsonPrimitive)?.content ?: messageId
            "waypoints" -> {
              if (element is JsonArray) {
                element.forEach { waypointJson ->
                  try {
                    waypoints.add(
                        jsonDecoder.json.decodeFromJsonElement(
                            MessageWaypoint.serializer(), waypointJson))
                  } catch (e: SerializationException) {
                    Timber.w("Failed to deserialize waypoint: $e")
                  }
                }
              }
            }
            else -> {
              if (element is JsonPrimitive) {
                when {
                  element.isString -> set(key, element.content)
                  element.booleanOrNull != null -> set(key, element.booleanOrNull)
                  element.longOrNull != null -> set(key, element.longOrNull!!.toInt())
                  element.doubleOrNull != null -> set(key, element.doubleOrNull)
                }
              }
            }
          }
        }
      }
    }
  }
}
