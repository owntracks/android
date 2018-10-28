package org.owntracks.android.services;

import android.support.annotation.CallSuper;

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

public abstract class MessageProcessorEndpoint implements OutgoingMessageProcessor {
    protected MessageProcessor messageProcessor;

    MessageProcessorEndpoint(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    void onMessageReceived(MessageBase message) {
        message.setModeId(getModeId());
        messageProcessor.onMessageReceived(onFinalizeMessage(message));
    }

    protected abstract MessageBase onFinalizeMessage(MessageBase message);
    abstract int getModeId();
}
