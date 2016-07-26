package org.owntracks.android.support.interfaces;


import org.owntracks.android.messages.MessageBase;

public interface MessageSender {
    void sendMessage(MessageBase message);
    void onMessageDelivered(Long messageID);
    void onMessageDeliveryFailed(Long messageID); // Server didn't return a return code
}
