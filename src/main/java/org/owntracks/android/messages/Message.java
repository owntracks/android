package org.owntracks.android.messages;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.owntracks.android.support.MessageLifecycleCallbacks;

public class Message extends MqttMessage {
    private static final String TAG = "Message";

    private MessageLifecycleCallbacks callback;
    private String topic;
    private boolean isPublishing;
    private Object extra;
    protected int ttl = 5; // Publishes to live. Decremented with each publish. Message is discarded after ptl reaches 0
    private boolean wasQueued = false;
    public Message() {
        super();
    }

    public Message(String topic, String payload, int qos, boolean retained, MessageLifecycleCallbacks callback, Object extra) {
        super(payload.getBytes());
        this.setQos(qos);
        this.setRetained(retained);
        this.extra = extra;
        this.callback = callback;
        this.topic = topic;
    }


    public void publishFailed() {
        if (this.callback != null)
            this.callback.onMessagePublishFailed(this.extra);
    }

    public void publishQueued() {
        wasQueued = true;
        if (this.callback != null)
            this.callback.onMessagePublishQueued(this.extra);

    }

    public void publishSuccessful() {
        if (this.callback != null) {
            Log.v(TAG, "Callback: " + this.callback);
            this.callback.onMessagePublishSuccessful(this.extra, this.wasQueued);
        } else
            Log.v(TAG, "message has no callback");

    }

    public void publishing() {
        this.isPublishing = true;
        if (this.callback != null)
            this.callback.onMesssagePublishing(this.extra);
    }

    public boolean isPublishing() {
        return this.isPublishing;
    }

    public void setCallback(MessageLifecycleCallbacks c) {this.callback = c;}
    public String getTopic() {
        return this.topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
    public synchronized int getTTL() {
        return ttl;
    }
    public synchronized int decrementTTL() {
        return --ttl;
    }


    public void setExtra(Object extra) {
        this.extra = extra;
    }


}
