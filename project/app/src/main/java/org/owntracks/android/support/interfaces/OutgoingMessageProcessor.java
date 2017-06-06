package org.owntracks.android.support.interfaces;


import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.services.MessageProcessor;

public interface OutgoingMessageProcessor {
    void processOutgoingMessage(MessageBase message);
    void processOutgoingMessage(MessageCmd message);
    void processOutgoingMessage(MessageEvent message);
    void processOutgoingMessage(MessageLocation message);
    void processOutgoingMessage(MessageTransition message);
    void processOutgoingMessage(MessageWaypoint message);
    void processOutgoingMessage(MessageWaypoints message);
    void processOutgoingMessage(MessageClear message);

    void onCreateFromProcessor();
    void onDestroy();
    void onEnterForeground();
}
