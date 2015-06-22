package org.owntracks.android.messages;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.Preferences;

public class LocationMessage extends Message{
    private static final String TAG = "LocationMessage";

    private GeocodableLocation location;
    private Waypoint waypoint;
    private String description;
    private String trackerId;
    private String trigger;
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

    }

    // For outgoing messages
    public LocationMessage(GeocodableLocation l) {
        super();
		this.location = l;
		this.battery = Preferences.getPubLocationIncludeBattery() ? App.getBatteryLevel() : -1;;
		this.waypoint = null;
		this.supressesTicker = false;
        this.trackerId = null;
        this.trigger = null;
        this.trackerId = Preferences.getTrackerId(true);
	}

	public boolean getSupressTicker() {
		return this.supressesTicker;
	}
	public void setSupressesTicker(boolean supressesTicker) {
		this.supressesTicker = supressesTicker;
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
            .put("tst", (TimeUnit.MILLISECONDS.toSeconds((new Date()).getTime())))
            .put("acc", Math.floor(this.location.getLocation().getAccuracy()));

            if (this.battery != -1)
                json.put("batt", this.battery);

            if(trigger != null)
                json.put("t", trigger);

            if (this.trackerId != null && !this.trackerId.isEmpty())
                 json.put("tid", this.trackerId);

        } catch (JSONException e) {

        }
        return json;
    }
}
