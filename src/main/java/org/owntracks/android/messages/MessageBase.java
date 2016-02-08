package org.owntracks.android.messages;
import android.databinding.BaseObservable;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import java.lang.ref.WeakReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true) // Don't fail on deserialization if an unknown attribute is encountered
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't serialize attributes with null values
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type", defaultImpl = MessageUnknown.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value=MessageLocation.class, name="location"),
        @JsonSubTypes.Type(value=MessageTransition.class, name="transition"),
        @JsonSubTypes.Type(value=MessageEvent.class, name="event"),
        @JsonSubTypes.Type(value=MessageMsg.class, name="msg"),
        @JsonSubTypes.Type(value=MessageCard.class, name="card"),
        @JsonSubTypes.Type(value=MessageCmd.class, name="cmd"),
        @JsonSubTypes.Type(value=MessageEncrypted.class, name="encrypted")

})

public abstract class MessageBase extends BaseObservable implements Runnable{
        protected static final String TAG = "MessageBase";
        protected String _mqtt_topic;
        protected Boolean record;

        @JsonIgnore
        protected int _mqtt_qos;

        @JsonIgnore
        protected boolean _mqtt_retained;

        @JsonIgnore
        public boolean getRetained() {
                return _mqtt_retained;
        }

        @JsonIgnore
        public void setRetained(boolean _mqtt_retained) {
                this._mqtt_retained = _mqtt_retained;
        }
        @JsonIgnore
        public int getQos() {
                return _mqtt_qos;
        }

        @JsonIgnore
        public void setQos(int _mqtt_qos) {
                this._mqtt_qos = _mqtt_qos;
        }

        @JsonIgnore
        protected WeakReference<IncomingMessageProcessor> _processorIn;

        @JsonIgnore
        protected WeakReference<OutgoingMessageProcessor> _processorOut;

        @JsonIgnore
        public String getTopic() {
                return _mqtt_topic;
        }

        @JsonIgnore
        public void setTopic(String _topic) {
                this._mqtt_topic = _topic;
        }

        @Override
        public void run(){
                if(_processorIn != null && _processorIn.get() !=  null)
                        processIncomingMessage(_processorIn.get());
                if(_processorOut != null && _processorOut.get() !=  null)
                        processOutgoingMessage(_processorOut.get());
        }

        @JsonIgnore
        public void setIncomingProcessor(IncomingMessageProcessor processor) {
                this._processorIn = new WeakReference<>(processor);
        }

        @JsonIgnore
        public void setOutgoingProcessor(OutgoingMessageProcessor processor) {
                this._processorOut = new WeakReference<>(processor);
        }

        @JsonIgnore
        public abstract void processIncomingMessage(IncomingMessageProcessor handler);

        @JsonIgnore
        public abstract void processOutgoingMessage(OutgoingMessageProcessor handler);

        @JsonIgnore
        public abstract String getBaseTopicSuffix();

        public Boolean getRecord() {
                return record;
        }

        public void setRecord(Boolean record) {
                this.record = record;
        }
}
