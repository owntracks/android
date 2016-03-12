package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageCmd extends MessageBase{
    private static final String BASETOPIC_SUFFIX = "/cmd";
    private String action;

    @Override
    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
    public boolean isValidMessage() {
        return super.isValidMessage() && (action != null);
    }

}
