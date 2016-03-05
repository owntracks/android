package org.owntracks.android.support.receiver;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageEncrypted;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.IncomingMessageProcessor;

import java.io.IOException;

public class Parser {
    private static final String TAG = "Parser";
    static ObjectMapper mapper;

    public static void initialize(Context c) {
        mapper = new ObjectMapper();

    }

    public static MessageBase deserializeSync(@NonNull byte[] json) throws IOException, EncryptionException {
        MessageBase m = mapper.readValue(json, MessageBase.class);

        if(m instanceof MessageEncrypted) {
            if(!EncryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");

            String decrypted = EncryptionProvider.decrypt(((MessageEncrypted) m).getData());
            m = mapper.readValue(decrypted, MessageBase.class);
        }

        return m;
    }

    public static String serializeSync(@NonNull MessageBase message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }



    private static class EncryptionException extends Exception {
        public EncryptionException(String s) {
            super(s);
        }
    }
}
