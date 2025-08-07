package org.owntracks.android.model

import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Base64
import org.owntracks.android.preferences.Preferences

@Singleton
class EncryptionProvider @Inject constructor(private val preferences: Preferences) {

  var isPayloadEncryptionEnabled: Boolean = false
    private set

  private val key = ByteArray(KEYBYTES)

  init {
    preferences.registerOnPreferenceChangedListener(SecretBoxManager())
    initializeSecretBox()
  }

  private fun initializeSecretBox() {
    val keyString = preferences.encryptionKey
    isPayloadEncryptionEnabled = keyString.isNotEmpty()

    keyString.toByteArray().copyOf(KEYBYTES).copyInto(key)
  }

  fun decrypt(cypherTextBase64: String): String {
    val nonceMacCiphertext = Base64.decode(cypherTextBase64)

    // Separate nonce, mac and ciphertext
    val poly1305 = Poly1305()
    val nonce = Arrays.copyOfRange(nonceMacCiphertext, 0, NONCEBYTES)
    val mac = Arrays.copyOfRange(nonceMacCiphertext, NONCEBYTES, NONCEBYTES + poly1305.macSize)
    val ciphertext =
        Arrays.copyOfRange(
            nonceMacCiphertext, NONCEBYTES + poly1305.macSize, nonceMacCiphertext.size)

    val xSalsa20Engine =
        XSalsa20Engine().apply { init(false, ParametersWithIV(KeyParameter(key), nonce)) }

    // Generate mac key
    val macKey = ByteArray(KEYBYTES).also { xSalsa20Engine.processBytes(it, 0, it.size, it, 0) }

    // Calculate Mac
    val macCalculated =
        ByteArray(poly1305.macSize).also {
          poly1305.init(KeyParameter(macKey))
          poly1305.update(ciphertext, 0, ciphertext.size)
          poly1305.doFinal(it, 0)
        }

    // Decrypt on successful authentication
    var decrypted: ByteArray? = null
    if (MessageDigest.isEqual(macCalculated, mac)) {
      decrypted =
          ByteArray(ciphertext.size).also {
            xSalsa20Engine.processBytes(ciphertext, 0, ciphertext.size, it, 0)
          }
    }

    return decrypted?.let { String(it) } ?: "" // Or throw an exception for failed decryption
  }

  fun encrypt(plaintext: String): String {
    return encrypt(plaintext.toByteArray()) // Uses default Charset
  }

  fun encrypt(plaintext: ByteArray): String {
    // Generate random nonce
    val nonce = ByteArray(NONCEBYTES).also { SecureRandom().nextBytes(it) }

    val xSalsa20Engine =
        XSalsa20Engine().apply { init(true, ParametersWithIV(KeyParameter(key), nonce)) }

    // Generate mac key
    val macKey = ByteArray(KEYBYTES).also { xSalsa20Engine.processBytes(it, 0, it.size, it, 0) }

    // Encrypt plaintext
    val ciphertext =
        ByteArray(plaintext.size).also {
          xSalsa20Engine.processBytes(plaintext, 0, plaintext.size, it, 0)
        }

    // Generate mac
    val poly1305 = Poly1305()
    val mac =
        ByteArray(poly1305.macSize).also {
          poly1305.init(KeyParameter(macKey))
          poly1305.update(ciphertext, 0, plaintext.size) // ciphertext size = plaintext size
          poly1305.doFinal(it, 0)
        }

    // Concatenate, e.g. nonce|mac|ciphertext
    val outBytes = Arrays.concatenate(nonce, mac, ciphertext)

    return String(Base64.encode(outBytes))
  }

  private inner class SecretBoxManager : Preferences.OnPreferenceChangeListener {
    // Kotlin automatically registers the listener due to the `init` block of the outer class.
    // If this class were to be instantiated elsewhere and then registered,
    // you might need an explicit registration call in its own init or constructor.
    // For this specific structure, the outer class's init handles it.

    override fun onPreferenceChanged(properties: Set<String>) {
      if (properties.contains("encryptionKey")) {
        initializeSecretBox()
      }
    }
  }

  companion object {
    const val NONCEBYTES = 24
    const val KEYBYTES = 32
  }
}
