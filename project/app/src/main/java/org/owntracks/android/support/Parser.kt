package org.owntracks.android.support

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageEncrypted
import org.owntracks.android.model.messages.MessageUnknown

@Singleton
class Parser @Inject constructor(private val encryptionProvider: EncryptionProvider?) {
    private val defaultMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    private val arrayCompatMapper = ObjectMapper()
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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
    fun fromJson(input: ByteArray): MessageBase =
        try {
            decrypt(fromUnencryptedJson(input))
        } catch (e: JsonMappingException) {
            MessageUnknown
        } catch (e: JsonParseException) {
            MessageUnknown
        }


    // Accepts 1) [{plain},{plain},...], 2) {plain}, 3) {encrypted, data:[{plain}, {plain}, ...]} as input stream
    @Throws(IOException::class, EncryptionException::class)
    fun fromJson(input: InputStream): Array<MessageBase> {
        return decrypt(arrayCompatMapper.readValue(input, Array<MessageBase>::class.java))
    }

    @Throws(IOException::class, EncryptionException::class)
    private fun decrypt(a: Array<MessageBase>?): Array<MessageBase> {
        // Recorder compatibility, encrypted messages with data array
        if (a == null) throw IOException("null array")
        return if (a.size == 1 && a[0] is MessageEncrypted) {
            if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled) {
                throw EncryptionException(
                    "received encrypted message but payload encryption is not enabled"
                )
            }
            defaultMapper.readValue(
                encryptionProvider.decrypt((a[0] as MessageEncrypted).data),
                Array<MessageBase>::class.java
            )
        } else { // single message wrapped in array by mapper or array of messages
            a
        }
    }

    @Throws(IOException::class, EncryptionException::class)
    private fun decrypt(m: MessageBase): MessageBase {
        if (m is MessageEncrypted) {
            if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled) {
                throw EncryptionException(
                    "received encrypted message but payload encryption is not enabled"
                )
            }
            return defaultMapper.readValue(
                encryptionProvider.decrypt(m.data),
                MessageBase::class.java
            )
        }
        return m
    }

    @Throws(IOException::class)
    private fun encryptString(input: String): String {
        if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled) {
            val m = MessageEncrypted()
            m.data = encryptionProvider.encrypt(input)
            return defaultMapper.writeValueAsString(m)
        }
        return input
    }

    @Throws(IOException::class)
    private fun encryptBytes(input: ByteArray): ByteArray {
        if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled) {
            val m = MessageEncrypted()
            m.data = encryptionProvider.encrypt(input)
            return defaultMapper.writeValueAsBytes(m)
        }
        return input
    }

    class EncryptionException internal constructor(s: String?) : Exception(s)
}
