package org.owntracks.android.model

import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageEncrypted
import org.owntracks.android.model.messages.MessageUnknown

@Singleton
class Parser @Inject constructor(private val encryptionProvider: EncryptionProvider?) {
  private val defaultMapper = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    classDiscriminator = "_type"
  }

  private val arrayCompatMapper = Json {
    ignoreUnknownKeys = true
    isLenient = true
    classDiscriminator = "_type"
  }

  @Throws(IOException::class)
  fun toUnencryptedJsonPretty(message: MessageBase): String {
    return defaultMapper.encodeToString(message).replace("\\r\\n".toRegex(), "\n")
  }

  @Throws(IOException::class)
  fun toJsonPlain(message: MessageBase): String {
    return defaultMapper.encodeToString(message)
  }

  @Throws(IOException::class)
  fun toUnencryptedJsonBytes(message: MessageBase): ByteArray {
    return defaultMapper.encodeToString(message).toByteArray()
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
    return decrypt(arrayCompatMapper.decodeFromString<Array<MessageBase>>(input.bufferedReader().use { it.readText() }))
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
