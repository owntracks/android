package org.owntracks.android.support;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;

public interface IncomingMessageProcessor {
    void processMessage(MessageBase message);
    void processMessage(MessageLocation message);
    void processMessage(MessageCard message);
    void processMessage(MessageCmd message);
    void processMessage(MessageTransition message);
    void processMessage(MessageUnknown message);

}
