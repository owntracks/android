package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.IncomingMessageProcessor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageWaypoint extends MessageBase {
    static final String TYPE = "waypoint";
    private static final String BASETOPIC_SUFFIX = "/event";
    private String desc;
    private double lon;
    private double lat;
    private long tst;
    // Optional types for optional values
    private Integer rad;

    public String getBaseTopicSuffix() {
        return BASETOPIC_SUFFIX;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public Integer getRad() {
        return rad;
    }

    public void setRad(Integer rad) {
        this.rad = rad;
    }

    public long getTst() {
        return tst;
    }

    public void setTst(long tst) {
        this.tst = tst;
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    @Override
    public boolean isValidMessage() {
        return super.isValidMessage() && (desc != null);
    }

    @Override
    public void addMqttPreferences(Preferences preferences) {
        setTopic(preferences.getPubTopicWaypoints());
        setQos(preferences.getPubQosWaypoints());
        setRetained(preferences.getPubRetainWaypoints());
    }

}
