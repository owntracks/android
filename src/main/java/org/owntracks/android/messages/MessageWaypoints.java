package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.OutgoingMessageProcessor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageWaypoints extends MessageBase{

    private MessageWaypointCollection waypoints;

    public MessageWaypointCollection getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(MessageWaypointCollection waypoints) {
        this.waypoints = waypoints;
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public String getBaseTopicSuffix() {  return null; }

}
