package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;


@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageUnknown extends MessageBase {

    public String getBaseTopicSuffix() {  return null; }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processOutgoingMessage(this);
    }

}
