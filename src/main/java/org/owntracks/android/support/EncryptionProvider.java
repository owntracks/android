package org.owntracks.android.support;


import android.util.Base64;
import android.util.Log;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.crypto.Util;

import java.util.Arrays;



public class EncryptionProvider {
    private static final String TAG = "EncryptionProvider";
    private static final int crypto_secretbox_NONCEBYTES = org.abstractj.kalium.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES;
    private static final int crypto_secretbox_KEYBYTES = org.abstractj.kalium.SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES;

    private static SecretBox b;
    private static Random r;
    private static byte[] key;

    public static void initialize() {
        key = new byte[crypto_secretbox_KEYBYTES];

        String k = "s3cr1t"; // TODO: FOR TESTING ONLY, GET FROM PREFERENCES

        System.arraycopy(k.getBytes(), 0, key, 0, k.length());
        Log.v(TAG, "using key to init box: " + key);
        b = new SecretBox(key);
        r = new Random();
    }
    public static String decrypt(String cyphertextb64) {

        Log.v(TAG, "decrypt: encoded cyphertext: " + cyphertextb64);

        byte[] onTheWire  =  Base64.decode(cyphertextb64.getBytes(), Base64.DEFAULT );
        byte[] nonce = new byte[crypto_secretbox_NONCEBYTES];
        byte[] cyphertext = new byte[onTheWire.length-crypto_secretbox_NONCEBYTES];

        System.arraycopy(onTheWire , 0, nonce, 0, crypto_secretbox_NONCEBYTES);
        System.arraycopy(onTheWire, crypto_secretbox_NONCEBYTES, cyphertext, 0, onTheWire.length - crypto_secretbox_NONCEBYTES);

        Log.v(TAG, "using nonce (b64) to decrypt: " + Base64.encodeToString(nonce, Base64.NO_WRAP));

        String plaintext = new String(b.decrypt(nonce, cyphertext));
        Log.v(TAG, "decrypt: plaintext: " + plaintext);
        return plaintext;
    }
    public static String encrypt(String plaintext) {
        Log.v(TAG, "encrypt: plaintext: " + plaintext);
        byte[] nonce = r.randomBytes(crypto_secretbox_NONCEBYTES);
        byte[] cypthertext = b.encrypt(nonce, plaintext.getBytes());
        byte[] out = new byte[crypto_secretbox_NONCEBYTES+cypthertext.length];

        System.arraycopy(nonce, 0, out, 0, crypto_secretbox_NONCEBYTES);
        System.arraycopy(cypthertext, 0, out, crypto_secretbox_NONCEBYTES, cypthertext.length);

        String b64 = Base64.encodeToString(out, Base64.NO_WRAP);

        Log.v(TAG, "using nonce (b64) to encrypt: " + Base64.encodeToString(nonce, Base64.NO_WRAP));

        Log.v(TAG, "encrypt: encoded out: " +out );

        return b64;
    }

}
