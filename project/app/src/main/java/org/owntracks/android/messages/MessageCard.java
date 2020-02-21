package org.owntracks.android.messages;

import androidx.databinding.Bindable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageCard extends MessageBase{
    static final String TYPE = "card";
    public static final String BASETOPIC_SUFFIX = "/info";
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
    public void addMqttPreferences(Preferences preferences) {

    }


    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
//        handler.processOutgoingMessage(this);
    }

}
