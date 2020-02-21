package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.android.gms.location.Geofence;

import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageTransition extends MessageBase{
    public static final String TYPE = "transition";
    private static final String BASETOPIC_SUFFIX = "/event";
    private static final String EVENT_ENTER = "enter";
    private static final String EVENT_LEAVE = "leave";

    public static final String TRIGGER_CIRCULAR = "c";
    public static final String TRIGGER_LOCATION = "l";


    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }
    @JsonIgnore
    private int  transition = 0;

    private String desc;
    private String tid;
    private String trigger;
    private String event;
    private long tst;
    private long wtst;
    private float acc;
    private double lon;
    private double lat;

    @JsonIgnore
    public int getTransition() {
        return transition;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    @Override
    public void addMqttPreferences(Preferences preferences) {
        setTopic(preferences.getPubTopicEvents());
        setQos(preferences.getPubQosEvents());
        setRetained(preferences.getPubRetainEvents());
    }

    @JsonProperty("t")
    public String getTrigger() {
        return trigger;
    }

    @JsonProperty("t")
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
        switch (event) {
            case EVENT_ENTER:
                transition = Geofence.GEOFENCE_TRANSITION_ENTER;
                break;
            case EVENT_LEAVE:
                transition = Geofence.GEOFENCE_TRANSITION_EXIT;
                break;
            default:
                transition = 0;
                break;
        }
    }

    @JsonIgnore
    public void setTransition(int transition) {
        this.transition = transition;
        switch (transition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                event = EVENT_ENTER;
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                event = EVENT_LEAVE;
                break;
            default:
                event = null;

        }
    }

    public long getTst() {
        return tst;
    }

    public void setTst(long tst) {
        this.tst = tst;
    }

    public long getWtst() {
        return wtst;
    }

    public void setWtst(long wtst) {
        this.wtst = wtst;
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
//        handler.processOutgoingMessage(this);
    }


    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setAcc(float acc) {
        this.acc = acc;
    }

    public float getAcc() {
        return acc;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

}
