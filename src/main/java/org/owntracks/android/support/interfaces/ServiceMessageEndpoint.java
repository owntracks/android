package org.owntracks.android.support.interfaces;

import android.support.v4.util.Pair;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageLocation;

public interface ServiceMessageEndpoint {
        boolean sendMessage(MessageBase message);
        void setMessageSenderCallback(MessageSender callback);
        void setMessageReceiverCallback(MessageReceiver callback);
        String getConnectionState();
        boolean acceptsMessages();
}
