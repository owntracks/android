package org.owntracks.android.messages;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import org.json.JSONObject;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.model.GeocodableLocation;

import android.util.Log;

import com.google.android.gms.location.Geofence;

public class LocationMessage extends Message{
    private GeocodableLocation location;
    private Waypoint waypoint;
    private String description;
    private String trackerId;
    private String trigger;
    private int transition;
    private int battery;
    private boolean supressesTicker;

    // For incoming messages
    public LocationMessage(JSONObject json) throws JSONException{
        this(new GeocodableLocation(json)); // new GeocodableLocation checks if json contains correct type element

        try {
            setBattery(json.getInt("batt"));
        } catch (Exception e) { }

        try {
            setDescription(json.getString("desc"));
        } catch (Exception e) { }

        try {
           setTrackerId(json.getString("tid"));
        } catch (Exception e) { }

        try {
            if (json.getString("event").equals("enter"))
                setTransition(Geofence.GEOFENCE_TRANSITION_ENTER);
            else if (json.getString("event").equals("exit"))
                setTransition(Geofence.GEOFENCE_TRANSITION_EXIT);
            else
                setTransition(-1);
        } catch (Exception e) { }
    }

    // For outgoing messages
    public LocationMessage(GeocodableLocation l) {
        super();
		this.location = l;
		this.transition = -1;
		this.battery = -1;
		this.waypoint = null;
		this.supressesTicker = false;
        this.trackerId = null;
        this.trigger = null;
	}

	public boolean getSupressTicker() {
		return this.supressesTicker;
	}
	public void setSupressesTicker(boolean supressesTicker) {
		this.supressesTicker = supressesTicker;
	}

    public Waypoint getWaypoint() {
        return this.waypoint;
    }
    public void setWaypoint(Waypoint waypoint) {
		this.waypoint = waypoint;
	}

    public void setTransition(int transition) {
        this.transition = transition;
    }
    public int getTransition() {
        return this.transition;
    }
    public boolean hasTransition() {
		return this.transition != -1;
	}

	public void setTrackerId(String tid) { this.trackerId = tid; }
	public String getTrackerId() { return this.trackerId; }

	public void setTrigger(String t) { this.trigger = t; }
	public String getTrigger() { return this.trigger; }

	public void setBattery(int battery) {
		this.battery = battery;
	}
    public int getBattery() {
        return this.battery;
    }

    private void setDescription(String string) {
        this.description = string;
    }
    public String getDescription() {
        return description;
    }

    public GeocodableLocation getLocation() {
        return this.location;
    }

    @Override
	public String toString() {
		return this.toJSONObject().toString();
	}

	public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            json.put("_type", "location")
            .put("lat", this.location.getLatitude())
            .put("lon", this.location.getLongitude())
            .put("tst", (TimeUnit.MILLISECONDS.toSeconds(this.location.getTime())))
            .put("acc", Math.round(this.location.getLocation().getAccuracy() * 100) / 100.0d);

            if (this.battery != -1)
                json.put("batt", this.battery);

            if ((this.waypoint != null) && ((this.transition == Geofence.GEOFENCE_TRANSITION_EXIT) || (this.transition == Geofence.GEOFENCE_TRANSITION_ENTER))) {
                if (this.waypoint.getShared())
                    json.put("desc", this.waypoint.getDescription());
                    json.put("event", this.transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "leave");
                }

                if(trigger != null)
                    json.put("t", trigger);

            if (this.trackerId != null && !this.trackerId.isEmpty())
                 json.put("tid", this.trackerId);

        } catch (JSONException e) {

        }
        return json;
    }
}
