package st.alr.mqttitude.messages;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.support.StringifiedJSONObject;

import android.util.Log;

import com.google.android.gms.location.Geofence;

public class LocationMessage {
	GeocodableLocation location;
	Waypoint waypoint;

	String description;
    String tid;
    String t;

    int transition;
	int battery;

	boolean supressesTicker;

	public LocationMessage(GeocodableLocation l) {
		this.location = l;
		this.transition = -1;
		this.battery = -1;
		this.waypoint = null;
		this.supressesTicker = false;
        this.tid = null;
        this.t = null;
	}

	public boolean doesSupressTicker() {
		return this.supressesTicker;
	}

	public void setSupressesTicker(boolean supressesTicker) {
		this.supressesTicker = supressesTicker;
	}

	public void setWaypoint(Waypoint waypoint) {
		this.waypoint = waypoint;
	}

	public boolean hasTransition() {
		return this.transition != -1;
	}

	public void setTransition(int transition) {
		this.transition = transition;
	}

    public void setTid(String tid) { this.tid = tid; }
    public String getTid() { return this.tid; }


    public void setT(String t) { this.t = t; }
    public String getT() { return this.t; }


    public void setBattery(int battery) {
		this.battery = battery;
	}

    @Override
    public String toString() {
        return this.toJSONObject().toString();
    }

	public StringifiedJSONObject toJSONObject() {
        StringifiedJSONObject json = new StringifiedJSONObject();

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

            if(t != null)
                json.put("t", t);

        } catch (JSONException e) {

        }

            return json;

	}

	public GeocodableLocation getLocation() {
		return this.location;
	}

	public Waypoint getWaypoint() {
		return this.waypoint;
	}

	public int getTransition() {
		return this.transition;
	}

	public int getBattery() {
		return this.battery;
	}

	public static LocationMessage fromJsonObject(StringifiedJSONObject json) {
		try {
			String type = json.getString("_type");
			if (!type.equals("location"))
				throw new JSONException("wrong type");
		} catch (JSONException e) {
			Log.e("LocationMessage",
					"Unable to deserialize LocationMessage object from JSON "
							+ json.toString());
			return null;
		}

		LocationMessage m = new LocationMessage(
				GeocodableLocation.fromJsonObject(json));

		try {
			m.setBattery(json.getInt("batt"));
		} catch (Exception e) {
		}

		try {
			m.setDescription(json.getString("desc"));
		} catch (Exception e) {
		}

        try {
            m.setTid(json.getString("tid"));
        } catch (Exception e) {
        }

		try {
			if (json.getString("event").equals("enter"))
				m.setTransition(Geofence.GEOFENCE_TRANSITION_ENTER);
			else if (json.getString("event").equals("exit"))
				m.setTransition(Geofence.GEOFENCE_TRANSITION_EXIT);
		} catch (Exception e) {
		}

		return m;

	}

	private void setDescription(String string) {
		this.description = string;
	}

}
