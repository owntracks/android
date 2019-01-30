package org.owntracks.android.messages;
import androidx.databinding.BaseObservable;
import androidx.annotation.NonNull;

import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import java.lang.ref.WeakReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type", defaultImpl = MessageUnknown.class)

@JsonSubTypes({
        @JsonSubTypes.Type(value=MessageLocation.class, name=MessageLocation.TYPE),
        @JsonSubTypes.Type(value=MessageTransition.class, name=MessageTransition.TYPE),
        @JsonSubTypes.Type(value=MessageEvent.class, name=MessageEvent.TYPE),
        @JsonSubTypes.Type(value=MessageCard.class, name=MessageCard.TYPE),
        @JsonSubTypes.Type(value=MessageCmd.class, name=MessageCmd.TYPE),
        @JsonSubTypes.Type(value=MessageConfiguration.class, name=MessageConfiguration.TYPE),
        @JsonSubTypes.Type(value=MessageEncrypted.class, name=MessageEncrypted.TYPE),
        @JsonSubTypes.Type(value=MessageWaypoint.class, name=MessageWaypoint.TYPE),
        @JsonSubTypes.Type(value=MessageWaypoints.class, name=MessageWaypoints.TYPE),
        @JsonSubTypes.Type(value=MessageLwt.class, name=MessageLwt.TYPE),
})
@JsonPropertyOrder(alphabetic=true)
public abstract class MessageBase extends BaseObservable implements Runnable {
        static final String TYPE = "base";

        @JsonIgnore
        protected String _topic;
        @JsonIgnore
        protected String _topic_base;

        @JsonIgnore
        private boolean delivered;

        @JsonIgnore
        private int modeId;

        @JsonIgnore
        public long getMessageId() {
                return _messageId;
        }

        @JsonIgnore
        private final Long _messageId = System.currentTimeMillis();

        @JsonIgnore
        private int _mqtt_qos;

        @JsonIgnore
        private boolean _mqtt_retained;
        private String tid;

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
        private WeakReference<IncomingMessageProcessor> _processorIn;

        @JsonIgnore
        private WeakReference<OutgoingMessageProcessor> _processorOut;

        @JsonIgnore
        @NonNull
        public String getContactKey() {
                if(_topic_base != null)
                        return _topic_base;
                if(tid != null)
                        return tid;
                return
                        "NOKEY";
        }

        public String getTopic() {
                return _topic;
        }

        @JsonIgnore
        public void setTopic(String topic) {
                this._topic = topic;
                this._topic_base = getBaseTopic(topic); // Normalized topic for all message types
        }

        @Override
        public void run(){
                if(_processorIn != null && _processorIn.get() !=  null)
                        processIncomingMessage(_processorIn.get());
                if(_processorOut != null && _processorOut.get() !=  null) {
                        processOutgoingMessage(_processorOut.get());
                }
        }


        @JsonIgnore
        public void setIncomingProcessor(@NonNull IncomingMessageProcessor processor) {
                this._processorOut = null;
                this._processorIn = new WeakReference<>(processor);
        }

        @JsonIgnore
        public void setOutgoingProcessor(@NonNull OutgoingMessageProcessor processor) {
                this._processorIn = null;
                this._processorOut = new WeakReference<>(processor);
        }

        public void clearOutgoingProcessor() {
                this._processorOut = null;
        }

        @JsonIgnore
        protected abstract void processIncomingMessage(IncomingMessageProcessor handler);

        @JsonIgnore
        protected abstract void processOutgoingMessage(OutgoingMessageProcessor handler);

        @JsonIgnore
        public abstract String getBaseTopicSuffix();

        // Called after deserialization to check if all required attributes are set or not.
        // The message is discarded if false is returned.
        @JsonIgnore
        public boolean isValidMessage() {
                return true;
        }

        @JsonIgnore
        public boolean isIncoming() {
                return this._processorIn != null;
        }

        @JsonIgnore
        public boolean isOutgoing() {
                return this._processorOut != null;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getTid() {
                return tid;
        }

        @JsonIgnore
        public boolean hasTid() {
                return getTid() != null;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public void setTid(String tid) {
                this.tid = tid;
        }

        @JsonIgnore
        protected String getBaseTopic(String topic){

                if (this.getBaseTopicSuffix() != null && topic.endsWith(this.getBaseTopicSuffix())) {
                        return topic.substring(0, (topic.length() - this.getBaseTopicSuffix().length()));
                } else {
                        return topic;
                }
        }

        @JsonIgnore
        public void setDelivered(boolean delivered) {
                this.delivered = delivered;
        }

        @JsonIgnore
        public boolean isDelivered() {
                return delivered;
        }


        @JsonIgnore
        public void setModeId(int modeId) {
                this.modeId = modeId;
        }

        @JsonIgnore
        public int getModeId() {
                return this.modeId;
        }


}
