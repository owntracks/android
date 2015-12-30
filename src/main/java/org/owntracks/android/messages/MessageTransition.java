package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutoingMessageProcessor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageTransition extends MessageBase{
    public static final String BASETOPIC_SUFFIX = "/event";
    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }
    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutoingMessageProcessor handler) {
        handler.processMessage(this);
    }

}
