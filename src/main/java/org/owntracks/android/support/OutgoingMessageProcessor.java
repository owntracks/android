package org.owntracks.android.support;


import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.messages.MessageEncrypted;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;

public interface OutgoingMessageProcessor {
    void processOutgoingMessage(MessageBase message);
    void processOutgoingMessage(MessageCmd message);
    void processOutgoingMessage(MessageEvent message);
    void processOutgoingMessage(MessageLocation message);
    void processOutgoingMessage(MessageTransition message);
    void processOutgoingMessage(MessageWaypoint message);
    void processOutgoingMessage(MessageWaypoints message);


}
