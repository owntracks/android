package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageCmd extends MessageBase{
    public static final String BASETOPIC_SUFFIX = "/cmd";
    @Override
    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }
    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

}
