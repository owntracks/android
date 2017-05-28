package org.owntracks.android.support;


import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.services.Dispatcher;
import org.owntracks.android.services.ServiceEndpointMqtt;
import org.owntracks.android.services.ServiceMessageHttp;

public class OutgoingMessageProcessorHttp implements OutgoingMessageProcessor {
    @Override
    public void processOutgoingMessage(MessageBase message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageCmd message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageEvent message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageLocation message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageTransition message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoint message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoints message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageClear message) {
        scheduleMessage(message);
    }

    private void scheduleMessage(MessageBase m) {
        Dispatcher.getInstance().scheduleMessage(ServiceMessageHttp.httpMessageToBundle(m));
    }
}
