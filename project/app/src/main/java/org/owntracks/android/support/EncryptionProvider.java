package org.owntracks.android.support;


import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.SecretBox;
import org.owntracks.android.injection.scopes.PerApplication;


import javax.inject.Inject;

import timber.log.Timber;

import static org.libsodium.jni.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES;
import static org.libsodium.jni.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;

@PerApplication
public class EncryptionProvider {
    private static final String TAG = "EncryptionProvider";
    private static final int crypto_secretbox_NONCEBYTES = XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;
    private static final int crypto_secretbox_KEYBYTES = XSALSA20_POLY1305_SECRETBOX_KEYBYTES;

    private static SecretBox b;
    private static Random r;
    private static boolean enabled;
    private final Preferences preferences;

    boolean isPayloadEncryptionEnabled() {
        return enabled;
    }

    private void initializeSecretBox() {
        String encryptionKey = preferences.getEncryptionKey();


        enabled = encryptionKey != null && encryptionKey.length() > 0;
        Timber.v("encryption enabled: %s", enabled);
        if (!enabled) {
            return;
        }

        //byte[] key = new byte[crypto_secretbox_KEYBYTES];
        //sSystem.arraycopy(encryptionKey.getBytes(), 0, key, 0, encryptionKey.length());
        try {
            b = new SecretBox(encryptionKey.getBytes());
            r = new Random();
        } catch (NullPointerException e) {
            Timber.e("unable to load encryptionKey");
            enabled = false;
        }
    }

    @Inject
    public EncryptionProvider(Preferences preferences) {
        this.preferences = preferences;
        preferences.registerOnPreferenceChangedListener(new SecretBoxManager());
        initializeSecretBox();
    }

    String decrypt(String cyphertextb64) {
        byte[] onTheWire = Base64.decode(cyphertextb64.getBytes(), Base64.DEFAULT);
        byte[] nonce = new byte[crypto_secretbox_NONCEBYTES];
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

    private class SecretBoxManager implements Preferences.OnPreferenceChangedListener {
        SecretBoxManager() {
            preferences.registerOnPreferenceChangedListener(this);
        }

        @Override
        public void onAttachAfterModeChanged() {
            initializeSecretBox();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Preferences.Keys._ENCRYPTION_KEY.equals(key))
                initializeSecretBox();
        }
    }
}
