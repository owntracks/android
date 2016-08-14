package org.owntracks.android.support;

import org.owntracks.android.messages.MessageBase;

public interface MessageHandler {
    void onHandleIncomingMessage(MessageBase message);
    void onHandleOutgoingMessage(MessageBase message);
}

