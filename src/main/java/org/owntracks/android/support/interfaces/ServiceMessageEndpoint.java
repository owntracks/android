package org.owntracks.android.support.interfaces;

import org.owntracks.android.messages.MessageBase;

public interface ServiceMessageEndpoint {
        void sendMessage(MessageBase message);
        void setOnMessageDeliveredCallback(MessageSender callback);
        void setOnMessageQueuedCallback(MessageSender callback);
        void setOnMessageReceivedCallback(MessageReceiver callback);


}
