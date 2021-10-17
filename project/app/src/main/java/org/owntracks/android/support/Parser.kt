package org.owntracks.android.support

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageEncrypted
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Parser @Inject constructor(private val encryptionProvider: EncryptionProvider?) {
    private val defaultMapper = JsonMapper.builder()
            .addModule(KotlinModule())
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .build()
    private val arrayCompatMapper = JsonMapper.builder()
            .addModule(KotlinModule())
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()

    @Throws(IOException::class)
    fun toUnencryptedJsonPretty(message: MessageBase): String {
        return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message)
                .replace("\\r\\n".toRegex(), "\n")
    }

    @Throws(IOException::class)
    fun toJsonPlain(message: MessageBase): String {
        return defaultMapper.writeValueAsString(message)
    }

    @Throws(IOException::class)
    fun toUnencryptedJsonBytes(message: MessageBase): ByteArray {
        return defaultMapper.writeValueAsBytes(message)
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
        return decrypt(defaultMapper.readValue(input, MessageBase::class.java))
    }

    @Throws(IOException::class)
    fun fromUnencryptedJson(input: ByteArray): MessageBase =
            defaultMapper.readValue(input, MessageBase::class.java)

    // Accepts {plain} as byte array
    @Throws(IOException::class, EncryptionException::class)
    fun fromJson(input: ByteArray): MessageBase {
        return decrypt(fromUnencryptedJson(input))
    }

    // Accepts 1) [{plain},{plain},...], 2) {plain}, 3) {encrypted, data:[{plain}, {plain}, ...]} as input stream
    @Throws(IOException::class, EncryptionException::class)
    fun fromJson(input: InputStream): Array<MessageBase> {
        return decrypt(arrayCompatMapper.readValue(input, Array<MessageBase>::class.java))
    }

    @Throws(IOException::class, EncryptionException::class)
    private fun decrypt(arrayOfMessageBases: Array<MessageBase>): Array<MessageBase> {
        // Recorder compatibility, encrypted messages with data array
        return if (arrayOfMessageBases.size == 1 && arrayOfMessageBases[0] is MessageEncrypted) {
            if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled) throw EncryptionException(
                    "received encrypted message but payload encryption is not enabled"
            )
            val data = encryptionProvider.decrypt((arrayOfMessageBases[0] as MessageEncrypted).data!!)
            defaultMapper.readValue(
                    data,
                    Array<MessageBase>::class.java
            )
        } else { // single message wrapped in array by mapper or array of messages
            arrayOfMessageBases
        }
    }

    @Throws(IOException::class, EncryptionException::class)
    private fun decrypt(messageBase: MessageBase): MessageBase {
        if (messageBase is MessageEncrypted) {
            if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled) throw EncryptionException(
                    "received encrypted message but payload encryption is not enabled"
            )
            return defaultMapper.readValue(
                    encryptionProvider.decrypt(messageBase.data!!), MessageBase::class.java
            )
        }
        return messageBase
    }

    @Throws(IOException::class)
    private fun encryptString(input: String): String {
        if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled) {
            val messageEncrypted =
                    MessageEncrypted().apply { data = encryptionProvider.encrypt(input) }
            return defaultMapper.writeValueAsString(messageEncrypted)
        }
        return input
    }

    @Throws(IOException::class)
    private fun encryptBytes(input: ByteArray): ByteArray {
        if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled) {
            val m = MessageEncrypted().apply { data = encryptionProvider.encrypt(input) }
            return defaultMapper.writeValueAsBytes(m)
        }
        return input
    }

    class EncryptionException internal constructor(s: String?) : Exception(s)
}