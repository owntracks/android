package org.owntracks.android.messages;

import org.json.JSONException;

import java.util.concurrent.TimeUnit;

import org.owntracks.android.db.Waypoint;
import org.owntracks.android.support.StringifiedJSONObject;

public class WaypointMessage extends Message {
	Waypoint waypoint;
    String trackerId;
	public WaypointMessage(Waypoint w) {
        super();

        this.waypoint = w;
    	}

	@Override
	public String toString() {
        	return toJSONObject().toString();
	}

    public StringifiedJSONObject toJSONObject() {
        StringifiedJSONObject json = new StringifiedJSONObject();
        try {
            json.put("_type", "waypoint")
                    .put("desc", this.waypoint.getDescription())
                    .put("lat", this.waypoint.getLatitude())
                    .put("lon", this.waypoint.getLongitude())
                    .put("tst", (int) (TimeUnit.MILLISECONDS.toSeconds(this.waypoint.getDate().getTime())))
                    .put("rad", this.waypoint.getRadius() != null ? this.waypoint.getRadius() : 0);

            if (this.trackerId != null && !this.trackerId.isEmpty()) 
                json.put("tid", this.trackerId);

        }catch (JSONException e) {
        }

        return json;
    }

    public void setTrackerId(String tid) {
        this.trackerId = tid;
    }
    public String getTrackerId(){ return this.trackerId; }
}
