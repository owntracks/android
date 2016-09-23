package org.owntracks.android.support;


import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;


public class EncryptionProvider {
    private static final String TAG = "EncryptionProvider";
    private static final int crypto_secretbox_NONCEBYTES = org.abstractj.kalium.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;
    private static final int crypto_secretbox_KEYBYTES = org.abstractj.kalium.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES;

    private static SecretBox b;
    private static Random r;
    private static byte[] key;
    private static boolean enabled;

    public static boolean isPayloadEncryptionEnabled() {
        return enabled;
    }

    private static void initializeSecretBox() {
        String encryptionKey = Preferences.getEncryptionKey();
        enabled = encryptionKey != null && encryptionKey.length() > 0;
        Log.v(TAG, "initializeSecretBox() - encryption enabled: " +enabled);
        if (!enabled)
            return;


        key = new byte[crypto_secretbox_KEYBYTES];
        System.arraycopy(encryptionKey.getBytes(), 0, key, 0, encryptionKey.length());
        b = new SecretBox(key);
        r = new Random();
        Log.v(TAG, "SecretBox initialized");
    }

    public static void initialize() {
        Preferences.registerOnPreferenceChangedListener(new SecretBoxManager());
        initializeSecretBox();
    }

    public static String decrypt(String cyphertextb64) {
        byte[] onTheWire = Base64.decode(cyphertextb64.getBytes(), Base64.DEFAULT);
        byte[] nonce = new byte[crypto_secretbox_NONCEBYTES];
        byte[] cyphertext = new byte[onTheWire.length - crypto_secretbox_NONCEBYTES];

        System.arraycopy(onTheWire, 0, nonce, 0, crypto_secretbox_NONCEBYTES);
        System.arraycopy(onTheWire, crypto_secretbox_NONCEBYTES, cyphertext, 0, onTheWire.length - crypto_secretbox_NONCEBYTES);
        return new String(b.decrypt(nonce, cyphertext));
    }

    public static String encrypt(String plaintext) {
        byte[] nonce = r.randomBytes(crypto_secretbox_NONCEBYTES);
        byte[] cyphertext = b.encrypt(nonce, plaintext.getBytes());
        byte[] out = new byte[crypto_secretbox_NONCEBYTES + cyphertext.length];

        System.arraycopy(nonce, 0, out, 0, crypto_secretbox_NONCEBYTES);
        System.arraycopy(cyphertext, 0, out, crypto_secretbox_NONCEBYTES, cyphertext.length);

        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    private static class SecretBoxManager implements Preferences.OnPreferenceChangedListener {
        public SecretBoxManager() {
            Preferences.registerOnPreferenceChangedListener(this);
        }


        @Override
        public void onAttachAfterModeChanged() {
            initializeSecretBox();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(Preferences.Keys._ENCRYPTION_KEY.equals(key))
                initializeSecretBox();
        }
    }
}
