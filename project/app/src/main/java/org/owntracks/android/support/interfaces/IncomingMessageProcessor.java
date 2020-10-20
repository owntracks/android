package org.owntracks.android.support.interfaces;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;

public interface IncomingMessageProcessor {
    void processIncomingMessage(MessageBase message);

    void processIncomingMessage(MessageLocation message);

    void processIncomingMessage(MessageCard message);

    void processIncomingMessage(MessageCmd message);

    void processIncomingMessage(MessageTransition message);

    void processIncomingMessage(MessageUnknown message);

    void processIncomingMessage(MessageClear message);
}
