package org.owntracks.android.support

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.util.Base64
import org.libsodium.jni.SodiumConstants
import org.libsodium.jni.crypto.Random
import org.libsodium.jni.crypto.SecretBox
import org.owntracks.android.R
import timber.log.Timber
import java.lang.System.arraycopy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionProvider @Inject constructor(private val preferences: Preferences) :
        OnSharedPreferenceChangeListener {

    private lateinit var secretBox: SecretBox
    private val random: Random = Random()
    var isPayloadEncryptionEnabled = false

    init {
        preferences.registerOnPreferenceChangedListener(this)
    }

    private fun initializeSecretBox() {
        val encryptionKey = preferences.encryptionKey
        isPayloadEncryptionEnabled = encryptionKey.isNotEmpty()
        Timber.v("encryption enabled: %s", isPayloadEncryptionEnabled)
        if (isPayloadEncryptionEnabled) {
            val encryptionKeyBytes = encryptionKey.toByteArray()
            val encryptionKeyBytesPadded = ByteArray(CRYPTO_SECRETBOX_KEYBYTES)
            if (encryptionKeyBytes.isEmpty()) {
                Timber.e(
                        "encryption key is too short or too long. Has %s bytes",
                        encryptionKeyBytes.size
                )
                isPayloadEncryptionEnabled = false
            } else {
                var copyBytes = encryptionKeyBytes.size
                if (copyBytes > CRYPTO_SECRETBOX_KEYBYTES) {
                    copyBytes = CRYPTO_SECRETBOX_KEYBYTES
                }
                arraycopy(encryptionKeyBytes, 0, encryptionKeyBytesPadded, 0, copyBytes)
                secretBox = SecretBox(encryptionKeyBytesPadded)
            }
        }
    }

    fun decrypt(cypherTextBase64: String): String {
        val onTheWire = Base64.decode(cypherTextBase64.toByteArray(), Base64.DEFAULT)
        val nonce = ByteArray(CRYPTO_SECRETBOX_NONCEBYTES)
        if (onTheWire.size <= CRYPTO_SECRETBOX_NONCEBYTES) {
            throw Parser.EncryptionException("Message length shorter than nonce")
        }
        val cypherText = ByteArray(onTheWire.size - CRYPTO_SECRETBOX_NONCEBYTES)
        arraycopy(onTheWire, 0, nonce, 0, CRYPTO_SECRETBOX_NONCEBYTES)
        arraycopy(
                onTheWire,
                CRYPTO_SECRETBOX_NONCEBYTES,
                cypherText,
                0,
                onTheWire.size - CRYPTO_SECRETBOX_NONCEBYTES
        )
        return String(secretBox.decrypt(nonce, cypherText))
    }

    fun encrypt(plaintext: String): String {
        return encrypt(plaintext.toByteArray())
    }

    fun encrypt(plaintext: ByteArray): String {
        val nonce = random.randomBytes(CRYPTO_SECRETBOX_NONCEBYTES)
        val cypherText = secretBox.encrypt(nonce, plaintext)
        val out = ByteArray(CRYPTO_SECRETBOX_NONCEBYTES + cypherText.size)
        arraycopy(nonce, 0, out, 0, CRYPTO_SECRETBOX_NONCEBYTES)
        arraycopy(cypherText, 0, out, CRYPTO_SECRETBOX_NONCEBYTES, cypherText.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (preferences.getPreferenceKey(R.string.preferenceKeyEncryptionKey) == key) initializeSecretBox()
    }

    companion object {
        private const val CRYPTO_SECRETBOX_NONCEBYTES =
                SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES
        private const val CRYPTO_SECRETBOX_KEYBYTES =
                SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES
    }

}