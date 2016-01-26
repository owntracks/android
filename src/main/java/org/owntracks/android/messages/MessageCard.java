package org.owntracks.android.messages;

import android.databinding.Bindable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;

import java.lang.ref.WeakReference;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageCard extends MessageBase{
    public static final String BASETOPIC_SUFFIX = "/info";
    private WeakReference<FusedContact> _contact;
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

    public void setHasCachedFace() {
        this.hasCachedFace = true;
    }
    public boolean hasCachedFace() {
        return this.hasCachedFace;
    }

        public boolean hasName() {
        return this.name != null;
    }

    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }



    public void setContact(FusedContact contact) {
        this._contact = new WeakReference<FusedContact>(contact);
    }
    private void notifyContactPropertyChanged() {
        if(_contact != null && _contact.get() != null)
            this._contact.get().notifyMessageCardPropertyChanged();

    }
    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

}
