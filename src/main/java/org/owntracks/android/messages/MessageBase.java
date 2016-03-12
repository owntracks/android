package org.owntracks.android.messages;
import android.databinding.BaseObservable;
import android.util.Log;

import org.owntracks.android.support.CanceableRunnable;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.PausableThreadPoolExecutor;

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
        @JsonSubTypes.Type(value=MessageCard.class, name="card"),
        @JsonSubTypes.Type(value=MessageCmd.class, name="cmd"),
        @JsonSubTypes.Type(value=MessageConfiguration.class, name="configuration"),
        @JsonSubTypes.Type(value=MessageEncrypted.class, name="encrypted"),
        @JsonSubTypes.Type(value=MessageWaypoint.class, name="waypoint")

})

public abstract class MessageBase extends BaseObservable implements PausableThreadPoolExecutor.ExecutorRunnable {
        protected static final String TAG = "MessageBase";
        public static final String ACTION_REPORT_LOCATION = "reportLocation";


        private String _mqtt_topic;

        @JsonIgnore
        private int _mqtt_qos;

        @JsonIgnore
        private boolean _mqtt_retained;
        private volatile boolean cancelOnRun = false;

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
        public String getTopic() {
                return _mqtt_topic;
        }

        @JsonIgnore
        public void setTopic(String _topic) {
                this._mqtt_topic = _topic;
        }

        @Override
        public void run(){
                // If the message is enqueued to a ThreadPoolExecutor, stopping that executor results in the first queued message runnable being run
                // We check if the running thread is shutting down and don't submit that messagfe to the message handler
                if(cancelOnRun)
                        return;

                if(_processorIn != null && _processorIn.get() !=  null)
                        processIncomingMessage(_processorIn.get());
                if(_processorOut != null && _processorOut.get() !=  null)
                        processOutgoingMessage(_processorOut.get());
        }

        @Override
        public void cancelOnRun() {
                this.cancelOnRun = true;
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



}
