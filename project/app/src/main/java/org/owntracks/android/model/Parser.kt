package org.owntracks.android.model

import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.owntracks.android.model.messages.InstantEpochSecondsSerializer
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageCmd
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.model.messages.MessageEncrypted
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageLwt
import org.owntracks.android.model.messages.MessageStatus
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageUnknown
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.model.messages.MessageWaypointCollectionSerializer
import org.owntracks.android.model.messages.MessageWaypoints

@Singleton
class Parser @Inject constructor(private val encryptionProvider: EncryptionProvider?) {
  private val serializersModule = SerializersModule {
    contextual(MessageWaypointCollectionSerializer)
    contextual(kotlinx.datetime.Instant::class, InstantEpochSecondsSerializer)
    polymorphic(MessageBase::class) {
      subclass(MessageCard::class)
      subclass(MessageClear::class)
      subclass(MessageCmd::class)
      subclass(MessageConfiguration::class, MessageConfiguration.MessageConfigurationSerializer)
      subclass(MessageEncrypted::class)
      subclass(MessageLocation::class)
      subclass(MessageLwt::class)
      subclass(MessageStatus::class)
      subclass(MessageTransition::class)
      subclass(MessageWaypoint::class)
      subclass(MessageWaypoints::class)
    }
  }

  private val defaultMapper = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    classDiscriminator = "_type"
    serializersModule = this@Parser.serializersModule
  }

  private val compactMapper = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "_type"
    serializersModule = this@Parser.serializersModule
  }

  private val arrayCompatMapper = Json {
    ignoreUnknownKeys = true
    isLenient = true
    classDiscriminator = "_type"
    serializersModule = this@Parser.serializersModule
  }

  /**
   * Encodes a MessageBase using the given mapper. MessageConfiguration is encoded directly via its
   * own serializer (not polymorphically) to avoid the duplicate `_type` field that occurs when
   * kotlinx.serialization's polymorphic discriminator mechanism interacts with the manual
   * `put("_type", ...)` in MessageConfigurationSerializer.
   */
  private fun encode(mapper: Json, message: MessageBase): String =
      if (message is MessageConfiguration) {
        mapper.encodeToString(MessageConfiguration.MessageConfigurationSerializer, message)
      } else {
        mapper.encodeToString(message)
      }

  @Throws(IOException::class)
  fun toUnencryptedJsonPretty(message: MessageBase): String {
    return encode(defaultMapper, message).replace("\\r\\n".toRegex(), "\n")
  }

  @Throws(IOException::class)
  fun toJsonPlain(message: MessageBase): String {
    return encode(compactMapper, message)
  }

  @Throws(IOException::class)
  fun toUnencryptedJsonBytes(message: MessageBase): ByteArray {
    return encode(compactMapper, message).toByteArray()
  }

  @Throws(IOException::class)
  fun toJson(message: MessageBase): String {
    return encryptString(toJsonPlain(message))
  }

  @Throws(IOException::class)
  fun toJsonBytes(message: MessageBase): ByteArray {
    return encryptBytes(toUnencryptedJsonBytes(message))
  }

  @Throws(IOException::class, EncryptionException::class)
  fun fromJson(input: String): MessageBase {
    // Validate JSON syntax first; rethrow SerializationException if malformed
    val element =
        try {
          defaultMapper.parseToJsonElement(input)
        } catch (e: SerializationException) {
          throw e
        }
    // No _type discriminator present → treat as unknown message
    if (element !is JsonObject || !element.containsKey("_type")) {
      return MessageUnknown
    }
    return decrypt(defaultMapper.decodeFromString<MessageBase>(input))
  }

  @Throws(IOException::class)
  fun fromUnencryptedJson(input: ByteArray): MessageBase =
      defaultMapper.decodeFromString(input.toString(Charsets.UTF_8))

  // Accepts {plain} as byte array
  @Throws(IOException::class, EncryptionException::class)
  fun fromJson(input: ByteArray): MessageBase =
      try {
        decrypt(fromUnencryptedJson(input))
      } catch (e: Exception) {
        MessageUnknown
      }

  // Accepts 1) [{plain},{plain},...], 2) {plain}, 3) {encrypted, data:[{plain}, {plain}, ...]} as
  // input stream
  @Throws(IOException::class, EncryptionException::class)
  fun fromJson(input: InputStream): Array<MessageBase> {
    val text = input.bufferedReader().use { it.readText() }
    return try {
      decrypt(arrayCompatMapper.decodeFromString<Array<MessageBase>>(text))
    } catch (e: SerializationException) {
      try {
        decrypt(arrayOf(arrayCompatMapper.decodeFromString<MessageBase>(text)))
      } catch (e2: SerializationException) {
        throw IOException("Failed to parse JSON", e2)
      }
    }
  }

  @Throws(IOException::class, EncryptionException::class)
  private fun decrypt(a: Array<MessageBase>?): Array<MessageBase> {
    // Recorder compatibility, encrypted messages with data array
    if (a == null) throw IOException("null array")
    return if (a.size == 1 && a[0] is MessageEncrypted) {
      if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled) {
        throw EncryptionException(
            "received encrypted message but payload encryption is not enabled", null)
      }
      val encryptedMessage = a[0] as MessageEncrypted
      val decrypted: String = encryptionProvider.decrypt(encryptedMessage.data)
      fromJson(decrypted.byteInputStream())
    } else {
      a
    }
  }

  @Throws(EncryptionException::class)
  private fun decrypt(message: MessageBase): MessageBase {
    return if (message is MessageEncrypted) {
      if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled) {
        throw EncryptionException(
            "received encrypted message but payload encryption is not enabled", null)
      }
      val decrypted: String = encryptionProvider.decrypt(message.data)
      fromJson(decrypted)
    } else {
      message
    }
  }

  @Throws(IOException::class)
  private fun encryptString(string: String): String {
    return if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled) {
      val encrypted = MessageEncrypted()
      encrypted.data = encryptionProvider.encrypt(string)
      toJsonPlain(encrypted)
    } else {
      string
    }
  }

  @Throws(IOException::class)
  private fun encryptBytes(bytes: ByteArray): ByteArray {
    return if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled) {
      val encrypted = MessageEncrypted()
      encrypted.data = encryptionProvider.encrypt(bytes)
      toUnencryptedJsonBytes(encrypted)
    } else {
      bytes
    }
  }

  class EncryptionException internal constructor(s: String, cause: Throwable?) :
      Exception(s, cause) {
    constructor(s: String) : this(s, null)
  }
}
