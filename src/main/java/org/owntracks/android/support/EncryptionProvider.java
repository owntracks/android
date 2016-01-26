package org.owntracks.android.support;


import android.util.Log;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;

public class EncryptionProvider {
    private static final String TAG = "EncryptionProvider";
    private static SecretBox b;
    private static Random r;
    public static void initialize() {
       // b = new SecretBox(Preferences.getEncryptionKey().getBytes());
        // r = new Random();
    }
    public static String decrypt(String cyphertext) {
        return "";
    }
    public static String encrypt(String plaintext) {
       // byte[] nonce = r.randomBytes();
        //// String s =  new String(b.encrypt(nonce, plaintext.getBytes()));
        //Log.v(TAG, "encrypted string: " +s );
        // return s;
        return "";
    }

}
