package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutoingMessageProcessor;


@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageUnknown extends MessageBase {

    public String getBaseTopicSuffix() {  return null; }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutoingMessageProcessor handler) {
        handler.processMessage(this);
    }

}
