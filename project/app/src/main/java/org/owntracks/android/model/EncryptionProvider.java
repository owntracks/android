package org.owntracks.android.model;

import static org.libsodium.jni.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES;
import static org.libsodium.jni.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.SecretBox;
import org.owntracks.android.preferences.Preferences;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class EncryptionProvider {
    private static final int crypto_secretbox_NONCEBYTES = XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;
    private static final int crypto_secretbox_KEYBYTES = XSALSA20_POLY1305_SECRETBOX_KEYBYTES;

    private static SecretBox b;
    private static Random r;
    private static boolean enabled;
    private final Preferences preferences;

    public boolean isPayloadEncryptionEnabled() {
        return enabled;
    }

    private void initializeSecretBox() {
        String encryptionKey = preferences.getEncryptionKey();

        enabled = encryptionKey != null && !encryptionKey.isEmpty();
        Timber.v("encryption enabled: %s", enabled);
        if (!enabled) {
            return;
        }

        byte[] encryptionKeyBytes = encryptionKey != null ? encryptionKey.getBytes() : new byte[0];
        byte[] encryptionKeyBytesPadded = new byte[crypto_secretbox_KEYBYTES];

        if (encryptionKeyBytes.length == 0) {
            Timber.e("encryption key is too short or too long. Has %s bytes", encryptionKeyBytes.length);
            enabled = false;
            return;
        }
        int copyBytes = encryptionKeyBytes.length;
        if (copyBytes > crypto_secretbox_KEYBYTES) {
            copyBytes = crypto_secretbox_KEYBYTES;
        }

        System.arraycopy(encryptionKeyBytes, 0, encryptionKeyBytesPadded, 0, copyBytes);

        b = new SecretBox(encryptionKeyBytesPadded);
        r = new Random();
    }

    @Inject
    public EncryptionProvider(Preferences preferences) {
        this.preferences = preferences;
        preferences.registerOnPreferenceChangedListener(new SecretBoxManager());
        initializeSecretBox();
    }

    String decrypt(String cyphertextb64) throws Parser.EncryptionException {
        byte[] onTheWire = Base64.decode(cyphertextb64.getBytes(), Base64.DEFAULT);
        byte[] nonce = new byte[crypto_secretbox_NONCEBYTES];
        if (onTheWire.length <= crypto_secretbox_NONCEBYTES) {
            throw new Parser.EncryptionException("Message length shorter than nonce");
        }
        byte[] cyphertext = new byte[onTheWire.length - crypto_secretbox_NONCEBYTES];

        System.arraycopy(onTheWire, 0, nonce, 0, crypto_secretbox_NONCEBYTES);
        System.arraycopy(onTheWire, crypto_secretbox_NONCEBYTES, cyphertext, 0, onTheWire.length - crypto_secretbox_NONCEBYTES);
        return new String(b.decrypt(nonce, cyphertext));
    }

    String encrypt(@NonNull String plaintext) {
        return encrypt(plaintext.getBytes());
    }

    String encrypt(@NonNull byte[] plaintext) {
        byte[] nonce = r.randomBytes(crypto_secretbox_NONCEBYTES);
        byte[] cyphertext = b.encrypt(nonce, plaintext);
        byte[] out = new byte[crypto_secretbox_NONCEBYTES + cyphertext.length];

        System.arraycopy(nonce, 0, out, 0, crypto_secretbox_NONCEBYTES);
        System.arraycopy(cyphertext, 0, out, crypto_secretbox_NONCEBYTES, cyphertext.length);

        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    private class SecretBoxManager implements Preferences.OnPreferenceChangeListener {
        SecretBoxManager() {
            preferences.registerOnPreferenceChangedListener(this);
        }

        @Override
        public void onPreferenceChanged(@NonNull Set<String> properties) {
            if (properties.contains("encryptionKey")) {
                initializeSecretBox();
            }
        }
    }
}
