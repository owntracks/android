package org.owntracks.android.messages;

import android.databinding.Bindable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageCard extends MessageBase{
    private static final String BASETOPIC_SUFFIX = "/info";
    private String name;
    private String face;
    private boolean hasCachedFace;

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getFace() {
        return face;
    }

    @JsonSetter
    public void setFace(String face) {
        this.face = face;
    }

    public boolean hasFace() {
        return this.face != null;
    }

    public boolean hasName() {
        return this.name != null;
    }

    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }


    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processOutgoingMessage(this);
    }

}
