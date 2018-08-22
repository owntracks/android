package org.owntracks.android.services;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import javax.inject.Inject;

abstract class MessageProcessorEndpoint implements OutgoingMessageProcessor {
    @Inject
    protected MessageProcessor messageProcessor;

    void onMessageReceived(MessageBase message) {
        message.setModeId(getModeId());
        messageProcessor.onMessageReceived(onFinalizeMessage(message));
    }

    protected abstract MessageBase onFinalizeMessage(MessageBase message);
    abstract int getModeId();
}
