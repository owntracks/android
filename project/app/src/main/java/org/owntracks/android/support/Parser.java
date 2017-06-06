package org.owntracks.android.support;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageEncrypted;

import java.io.IOException;
import java.io.InputStream;

public class Parser {
    private static ObjectMapper defaultMapper;
    private static ObjectMapper arrayCompatMapper;
    private EncryptionProvider encryptionProvider;
    public Parser(EncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
        defaultMapper = new ObjectMapper();
        defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        defaultMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        arrayCompatMapper = new ObjectMapper();
        arrayCompatMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        arrayCompatMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String toJsonPlainPretty(@NonNull MessageBase message) throws IOException {
        return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
    }


    public String toJsonPlain(@NonNull MessageBase message) throws IOException {
        return defaultMapper.writeValueAsString(message);
    }

    public String toJson(@NonNull MessageBase message) throws IOException, EncryptionException {
        return encrypt(toJsonPlain(message));
    }

    // Accepts {plain} as byte array
    public MessageBase fromJson(@NonNull byte[] input) throws IOException, EncryptionException {
        return decrypt(defaultMapper.readValue(input, MessageBase.class));
    }

    // Accepts 1) [{plain},{plain},...], 2) {plain}, 3) {encrypted, data:[{plain}, {plain}, ...]} as input stream
    public MessageBase[] fromJson(@NonNull InputStream input ) throws IOException, EncryptionException {
        return decrypt(arrayCompatMapper.readValue(input, MessageBase[].class));
    }

    private MessageBase[] decrypt(MessageBase[] a) throws IOException, EncryptionException {
        // Recorder compatiblity, encrypted messages with data array
        if(a == null)
            throw new IOException("null array");

        if (a.length == 1 && a[0] instanceof MessageEncrypted) {
            if (!encryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return defaultMapper.readValue(encryptionProvider.decrypt(((MessageEncrypted) a[0]).getData()), MessageBase[].class);
        } else { // single message wrapped in array by mapper or array of messages
            return a;
        }
    }

    private MessageBase decrypt(MessageBase m) throws IOException, EncryptionException {
        if(m instanceof MessageEncrypted) {
            if(!encryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return defaultMapper.readValue(encryptionProvider.decrypt(((MessageEncrypted) m).getData()), MessageBase.class);
        }
        return m;
    }


    private String encrypt(@NonNull String input) throws IOException, EncryptionException {
        if(encryptionProvider.isPayloadEncryptionEnabled()) {
            MessageEncrypted m = new MessageEncrypted();
            m.setdata(encryptionProvider.encrypt(input));
            return defaultMapper.writeValueAsString(m);
        }
        return input;
    }


    public static class EncryptionException extends Exception {
        EncryptionException(String s) {
            super(s);
        }
    }
}
