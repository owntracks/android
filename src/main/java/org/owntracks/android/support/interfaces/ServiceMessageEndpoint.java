package org.owntracks.android.support.interfaces;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageLocation;

public interface ServiceMessageEndpoint {
        void sendMessage(MessageBase message);
        void setMessageSenderCallback(MessageSender callback);
        void setMessageReceiverCallback(MessageReceiver callback);
        String getStateAsString();
}
