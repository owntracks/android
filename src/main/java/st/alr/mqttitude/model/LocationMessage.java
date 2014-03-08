package st.alr.mqttitude.model;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import st.alr.mqttitude.db.Waypoint;
import android.util.Log;

import com.google.android.gms.location.Geofence;

public class LocationMessage {
	GeocodableLocation location;
	Waypoint waypoint;

	String description;
	int transition;
	int battery;

	boolean supressesTicker;

	public LocationMessage(GeocodableLocation l) {
		this.location = l;
		this.transition = -1;
		this.battery = -1;
		this.waypoint = null;
		this.supressesTicker = false;
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

	public void setBattery(int battery) {
		this.battery = battery;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("{");
		builder.append("\"_type\": ").append("\"").append("location")
				.append("\"");
		builder.append(", \"lat\": ").append("\"")
				.append(this.location.getLatitude()).append("\"");
		builder.append(", \"lon\": ").append("\"")
				.append(this.location.getLongitude()).append("\"");
		builder.append(", \"tst\": ")
				.append("\"")
				.append((int) (TimeUnit.MILLISECONDS.toSeconds(this.location
						.getTime()))).append("\"");
		builder.append(", \"acc\": ")
				.append("\"")
				.append(Math
						.round(this.location.getLocation().getAccuracy() * 100) / 100.0d)
				.append("\"");

		if (this.battery != -1)
			builder.append(", \"batt\": ").append("\"").append(this.battery)
					.append("\"");

		if ((this.waypoint != null)
				&& ((this.transition == Geofence.GEOFENCE_TRANSITION_EXIT) || (this.transition == Geofence.GEOFENCE_TRANSITION_ENTER))) {
			if (this.waypoint.getShared())
				builder.append(", \"desc\": ").append("\"")
						.append(this.waypoint.getDescription()).append("\"");
			builder.append(", \"event\": ")
					.append("\"")
					.append(this.transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter"
							: "leave").append("\"");
		}

		return builder.append("}").toString();

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

	public static LocationMessage fromJsonObject(JSONObject json) {
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
