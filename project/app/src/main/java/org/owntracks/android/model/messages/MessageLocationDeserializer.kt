package org.owntracks.android.model.messages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object MessageLocationDeserializer : KSerializer<MessageLocation> {
  private val defaultSerializer = MessageLocation.serializer()
  override val descriptor: SerialDescriptor = defaultSerializer.descriptor

  override fun serialize(encoder: Encoder, value: MessageLocation) {
    defaultSerializer.serialize(encoder, value)
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun deserialize(decoder: Decoder): MessageLocation {
    if (decoder !is JsonDecoder) {
      throw SerializationException("Only JSON decoder is supported")
    }

    val element = decoder.decodeJsonElement()
    require(element is JsonObject) { "Expected JSON object for MessageLocation" }

    val errors = mutableListOf<String>()
    if (!element.containsKey("tst")) errors.add("missing 'tst' (timestamp)")
    if (!element.containsKey("lat")) errors.add("missing 'lat' (latitude)")
    if (!element.containsKey("lon")) errors.add("missing 'lon' (longitude)")
    if (!element.containsKey("tid") && !element.containsKey("topic")) {
      errors.add("missing both 'tid' and 'topic' (at least one required)")
    }

    // Check for zero timestamp
    val tst = element["tst"]?.jsonPrimitive?.longOrNull
    if (tst == 0L) {
      errors.add("'tst' (timestamp) must be non-zero")
    }

    if (errors.isNotEmpty()) {
      throw SerializationException("Invalid location message: ${errors.joinToString(", ")}")
    }

    // Use the decoder's json instance to deserialize with the default serializer
    return decoder.json.decodeFromJsonElement(defaultSerializer, element)
  }
}
