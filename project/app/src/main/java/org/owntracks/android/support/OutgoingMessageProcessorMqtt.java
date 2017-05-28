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

import java.io.IOException;

public class OutgoingMessageProcessorMqtt implements OutgoingMessageProcessor {
    @Override
    public void processOutgoingMessage(MessageBase message) {
        message.setTopic(Preferences.getPubTopicBase());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageCmd message) {
        message.setTopic(Preferences.getPubTopicCommands());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageEvent message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageLocation message) {
        message.setTopic(Preferences.getPubTopicLocations());
        message.setQos(Preferences.getPubQosLocations());
        message.setRetained(Preferences.getPubRetainLocations());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageTransition message) {
        message.setTopic(Preferences.getPubTopicEvents());
        message.setQos(Preferences.getPubQosEvents());
        message.setRetained(Preferences.getPubRetainEvents());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoint message) {
        message.setTopic(Preferences.getPubTopicWaypoints());
        message.setQos(Preferences.getPubQosWaypoints());
        message.setRetained(Preferences.getPubRetainWaypoints());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoints message) {
        message.setTopic(Preferences.getPubTopicWaypoints());
        message.setQos(Preferences.getPubQosWaypoints());
        message.setRetained(Preferences.getPubRetainWaypoints());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageClear message) {
        //TODO
    }

    private void scheduleMessage(MessageBase m) {
        try {
            Dispatcher.getInstance().scheduleMessage(ServiceEndpointMqtt.mqttMessageToBundle(m));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Parser.EncryptionException e) {
            e.printStackTrace();
        }
    }
}
