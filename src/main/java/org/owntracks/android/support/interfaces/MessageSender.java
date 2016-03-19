package org.owntracks.android.support.interfaces;


import org.owntracks.android.messages.MessageBase;

public interface MessageSender {
    void sendMessage(MessageBase message);
    void onMessageDelivered(MessageBase message);
    void onMessageQueued(MessageBase message);
}
