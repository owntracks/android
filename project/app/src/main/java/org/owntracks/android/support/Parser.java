package org.owntracks.android.support;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageEncrypted;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class Parser {
    private static final String TAG = "Parser";
    private static ObjectMapper defaultMapper;
    private static ObjectMapper arrayCompatMapper;

    public static void initialize(Context c) {
        defaultMapper = new ObjectMapper();
        arrayCompatMapper = new ObjectMapper();
        arrayCompatMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    }
    public static String toJson(@NonNull MessageBase message) throws IOException, EncryptionException {
        return encrypt(defaultMapper.writeValueAsString(message));
    }

    // Accepts {plain} as byte array
    public static MessageBase fromJson(@NonNull byte[] input) throws IOException, EncryptionException {
        return decrypt(defaultMapper.readValue(input, MessageBase.class));
    }

    // Accepts 1) [{plain},{plain},...], 2) {plain}, 3) {encrypted, data:[{plain}, {plain}, ...]} as input stream
    public static MessageBase[] fromJson(@NonNull InputStream input ) throws IOException, EncryptionException {
        return decrypt(arrayCompatMapper.readValue(input, MessageBase[].class));
    }

    private static MessageBase[] decrypt(MessageBase[] a) throws IOException, EncryptionException {
        // Recorder compatiblity, encrypted messages with data array
        if (a.length == 1 && a[0] instanceof MessageEncrypted) {
            if (!EncryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return defaultMapper.readValue(EncryptionProvider.decrypt(((MessageEncrypted) a[0]).getData()), MessageBase[].class);
        } else { // single message wrapped in array by mapper or array of messages
            return a;
        }
    }

    private static MessageBase decrypt(MessageBase m) throws IOException, EncryptionException {
        if(m instanceof MessageEncrypted) {
            if(!EncryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return defaultMapper.readValue(EncryptionProvider.decrypt(((MessageEncrypted) m).getData()), MessageBase.class);
        }
        return m;
    }


    private static String encrypt(@NonNull String input) throws IOException, EncryptionException {
        if(EncryptionProvider.isPayloadEncryptionEnabled()) {
            Timber.v("encrypting outgoing message with payload:%s", input);
            MessageEncrypted m = new MessageEncrypted();
            m.setdata(EncryptionProvider.encrypt(input));
            return defaultMapper.writeValueAsString(m);
        }
        return input;
    }


    public static class EncryptionException extends Exception {
        public EncryptionException(String s) {
            super(s);
        }
    }
}
