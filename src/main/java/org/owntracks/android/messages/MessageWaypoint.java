package org.owntracks.android.messages;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private int rad;
    private long tst;
    private boolean shared;
    private String uuid;
    private int major;
    private int minor;

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

    public int getRad() {
        return rad;
    }

    public void setRad(int rad) {
        this.rad = rad;
    }

    public long getTst() {
        return tst;
    }

    public void setTst(long tst) {
        this.tst = tst;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
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

        w.setGeofenceLatitude(getLat());
        w.setGeofenceLongitude(getLon());
        w.setGeofenceRadius(getRad());

        if(getDesc().contains(":")) {
            String[] a = getDesc().split(":");

            w.setDescription(a[0]);
            if(a.length >= 2)
                w.setBeaconUUID(a[1]);
            if(a.length >= 3)
                try{ w.setBeaconMajor(Integer.parseInt(a[2]));} catch (NumberFormatException e){};
            if(a.length >= 4)
                try{ w.setBeaconMinor(Integer.parseInt(a[3]));} catch (NumberFormatException e){};

        } else {
            w.setDescription(getDesc());
        }

        w.setShared(isShared());
        w.setDate(new Date(TimeUnit.SECONDS.toSeconds(getTst())));

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
        return message;
    }


    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
