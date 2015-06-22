package org.owntracks.android.messages;


public class ClearMessage extends Message {
    private static final String TAG = "ClearMessage";


    public ClearMessage(String topic) {
        this.setTopic(topic);
        this.setRetained(true);
        this.setPayload(new byte[0]);
    }
}
