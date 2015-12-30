package org.owntracks.android.support;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageUnknown;

public interface MessageHandler {
    void onHandleIncomingMessage(MessageBase message);
    void onHandleOutgoingMessage(MessageBase message);
}

