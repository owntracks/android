package org.owntracks.android.messages;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.owntracks.android.support.MessageCallbacks;

public class Message extends MqttMessage {
    private MessageCallbacks callback;
    private String topic;
    private boolean isPublishing;
    private Object extra;
    private int ttl = 5; // Publishes to live. Decremented with each publish. Message is discarded after ptl reaches 0
    public Message() {
        super();
    }

    public Message(String topic, String payload, int qos, boolean retained, MessageCallbacks callback, Object extra) {
        super(payload.getBytes());
        this.setQos(qos);
        this.setRetained(retained);
        this.extra = extra;
        this.callback = callback;
        this.topic = topic;
    }


    public void publishFailed() {
        if (this.callback != null)
            this.callback.publishFailed(this.extra);
    }

    public void publishSuccessful() {
        if (this.callback != null) {
            Log.v(this.toString(), "Callback: " + this.callback);
            this.callback.publishSuccessfull(this.extra);
        } else
            Log.v(this.toString(), "message has no callback");

    }

    public void publishing() {
        this.isPublishing = true;
        if (this.callback != null)
            this.callback.publishing(this.extra);
    }

    public boolean isPublishing() {
        return this.isPublishing;
    }

    public void setCallback(MessageCallbacks c) {this.callback = c;}
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
