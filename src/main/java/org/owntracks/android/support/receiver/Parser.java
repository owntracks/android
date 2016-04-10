package org.owntracks.android.support.receiver;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageEncrypted;
import org.owntracks.android.support.EncryptionProvider;

import java.io.IOException;
import java.io.InputStream;

public class Parser {
    private static final String TAG = "Parser";
    static ObjectMapper mapper;

    public static void initialize(Context c) {
        mapper = new ObjectMapper();

    }



    private static MessageBase pipelineDecrypt(MessageBase m) throws IOException, EncryptionException {
        if(m instanceof MessageEncrypted) {
            if(!EncryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return mapper.readValue(EncryptionProvider.decrypt(((MessageEncrypted) m).getData()), MessageBase.class);
        }
        return m;
    }

    private static MessageBase pipelineDeserialize(InputStream input) throws IOException, EncryptionException {
        return mapper.readValue(input, MessageBase.class);
    }
    private static MessageBase pipelineDeserialize(@NonNull byte[] input) throws IOException, EncryptionException {
        return mapper.readValue(input, MessageBase.class);
    }

    public static MessageBase deserializeSync(@NonNull InputStream input ) throws IOException, EncryptionException {
        return pipelineDecrypt(pipelineDeserialize(input));
    }

    public static MessageBase deserializeSync(@NonNull byte[] input) throws IOException, EncryptionException {
        return pipelineDecrypt(pipelineDeserialize(input));
    }

    private static String pipelineSerialize(@NonNull MessageBase input) throws IOException, EncryptionException {
        return mapper.writeValueAsString(input);
    }

    private static String pipelineEncrypt(@NonNull String input) throws IOException, EncryptionException {
        if(EncryptionProvider.isPayloadEncryptionEnabled()) {
            MessageEncrypted m = new MessageEncrypted();
            m.setdata(EncryptionProvider.encrypt(input));
            return mapper.writeValueAsString(input);
        }
        return input;
    }

    public static String serializeSync(@NonNull MessageBase message) throws IOException, EncryptionException {
        return pipelineEncrypt(pipelineSerialize(message));
    }

    public static class EncryptionException extends Exception {
        public EncryptionException(String s) {
            super(s);
        }
    }
}
