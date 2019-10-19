package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.owntracks.android.support.interfaces.IncomingMessageProcessor;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageWaypoint extends MessageBase{
    static final String TYPE = "waypoint";
    private static final String BASETOPIC_SUFFIX = "/event";

    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }

    private String desc;
    private double lon;
    private double lat;
    private long tst;

    // Optional types for optional values
    private Integer rad;

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
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processOutgoingMessage(this);
    }

    @Override
    public boolean isValidMessage() {
        return super.isValidMessage() && (desc != null);
    }

}
