package org.owntracks.android.messages;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.gms.location.Geofence;

import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageWaypoint extends MessageBase{
    public static final String BASETOPIC_SUFFIX = "/event";

    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }

    private String desc;
    private double lon;
    private double lat;
    private long tst;

    // Optional types for optional values
    private Boolean shared;
    private Integer rad;
    private String uuid;
    private Integer major;
    private Integer minor;

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

    @JsonIgnore
    public boolean isShared() {
        return shared != null && shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }


    public Waypoint toDaoObject() {
        Waypoint w = new Waypoint();

        w.setDescription(getDesc());
        w.setGeofenceLatitude(getLat());
        w.setGeofenceLongitude(getLon());
        w.setGeofenceRadius(getRad());
        w.setBeaconUUID(getUuid());
        w.setBeaconMajor(getMajor());
        w.setBeaconMinor(getMinor());
        w.setShared(isShared());
        w.setDate(new Date(TimeUnit.SECONDS.toMillis(getTst())));

        return w;
    }

    public static MessageWaypoint fromDaoObject(Waypoint w) {
        MessageWaypoint message = new MessageWaypoint();
        message.setDesc(w.getDescription());
        message.setLat(w.getGeofenceLatitude());
        message.setLon(w.getGeofenceLongitude());
        message.setRad(w.getGeofenceRadius());
        message.setShared(w.getShared());
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));
        message.setUuid(w.getBeaconUUID());
        message.setMajor(w.getBeaconMajor());
        message.setMinor(w.getBeaconMinor());
        return message;
    }


    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean isValidMessage() {
        return super.isValidMessage() && (desc != null);
    }

}
