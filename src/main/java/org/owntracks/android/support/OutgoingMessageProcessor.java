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
    void processMessage(MessageBase message);
    void processMessage(MessageCmd message);
    void processMessage(MessageEvent message);
    void processMessage(MessageLocation message);
    void processMessage(MessageTransition message);
    void processMessage(MessageWaypoint message);
    void processMessage(MessageWaypoints message);


}
