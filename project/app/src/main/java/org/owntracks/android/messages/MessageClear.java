package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
public class MessageClear extends MessageBase {
    @Override
    protected void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    protected void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processOutgoingMessage(this);
    }

    @Override
    @JsonIgnore
    public String getBaseTopicSuffix() {
        return null;
    }
}
