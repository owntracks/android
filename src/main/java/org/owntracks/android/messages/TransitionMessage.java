package org.owntracks.android.messages;

import com.google.android.gms.location.Geofence;

import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.model.GeocodableLocation;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

public class TransitionMessage extends Message{
    private GeocodableLocation location;
    private Waypoint waypoint;
    private String description;
    private String trackerId;
    private String trigger;
    private int transition;
    private boolean supressesTicker;

    // For incoming messages
    public TransitionMessage(JSONObject json) throws JSONException{

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
    public TransitionMessage(Waypoint w) {
        super();
		this.transition = -1;
		this.supressesTicker = false;
        this.trackerId = null;
        this.trigger = null;
        this.waypoint = w;
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


    public boolean isForEnterTransition() {
        return hasTransition() && this.transition == Geofence.GEOFENCE_TRANSITION_ENTER;
    }


    public boolean isForExitTransition() {
        return hasTransition() && this.transition == Geofence.GEOFENCE_TRANSITION_EXIT;
    }

	public void setTrackerId(String tid) { this.trackerId = tid; }
	public String getTrackerId() { return this.trackerId; }

	public void setTrigger(String t) { this.trigger = t; }
	public String getTrigger() { return this.trigger; }

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
            json.put("_type", "transition")
            .put("lat", this.waypoint.getLatitude())
            .put("lon", this.waypoint.getLongitude())
            .put("tst", (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                    .put("rad", Math.round(this.location.getLocation().getAccuracy() * 100) / 100.0d);

            if ((this.waypoint != null) && ((this.transition == Geofence.GEOFENCE_TRANSITION_EXIT) || (this.transition == Geofence.GEOFENCE_TRANSITION_ENTER))) {
                if (this.waypoint.getShared())
                    json.put("desc", this.waypoint.getDescription());
                    json.put("event", this.transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "leave");
                }

            if (this.trackerId != null && !this.trackerId.isEmpty())
                 json.put("tid", this.trackerId);

        } catch (JSONException e) {

        }
        return json;
    }
}
