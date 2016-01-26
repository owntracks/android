package org.owntracks.android.support;


import org.owntracks.android.messages.MessageBase;

public interface OutgoingMessageProcessor {
    void processMessage(MessageBase message);
}
