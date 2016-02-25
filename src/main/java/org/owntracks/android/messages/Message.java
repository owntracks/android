package org.owntracks.android.messages;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Message extends MqttMessage {
    private static final String TAG = "Message";
    private Object extra = null;

    private String topic;
    public Message() {
        super();
    }

    public Message(String topic, String payload, int qos, boolean retained, Object extra) {
        super(payload.getBytes());
        this.setQos(qos);
        this.setRetained(retained);
        this.extra = extra;
        this.topic = topic;
    }




    public String getTopic() {
        return this.topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }


}
