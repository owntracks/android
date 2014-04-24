package st.alr.mqttitude.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.support.Preferences;
import st.alr.mqttitude.support.StringifiedJSONObject;

public class ConfigurationMessage {
    StringifiedJSONObject json;

    public ConfigurationMessage() {
        this.json = Preferences.toJSONObject();
    }
    public String toString() {return json.toString(); }
    public StringifiedJSONObject toJSONObject() {
        return json;
    }


    public void addWaypoints() {
        JSONArray waypoints = new JSONArray();

        for(Waypoint waypoint : App.getWaypointDao().loadAll()) {
            StringifiedJSONObject w = new StringifiedJSONObject();
            try { w.put("_type", "waypoint"); } catch (JSONException e) { }
            try { w.put("tst", waypoint.getDate().getTime()); } catch (JSONException e) { }
            try { w.put("lat", waypoint.getLatitude()); } catch (JSONException e) { }
            try { w.put("lon", waypoint.getLongitude()); } catch (JSONException e) { }
            try { w.put("rad", waypoint.getRadius()); } catch (JSONException e) { }
            try { w.put("shared", waypoint.getShared() ? 1 : 0); } catch (JSONException e) { }
            try { w.put("desc", waypoint.getDescription()); } catch (JSONException e) { }
            try { w.put("transition", waypoint.getTransitionType()); } catch (JSONException e) { }
            waypoints.put(w);
        }

        try {
            json.put("waypoints", waypoints);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public void removeUsernamePassword() {
        json.remove(Preferences.getStringRessource(R.string.keyUsername));
        json.remove(Preferences.getStringRessource(R.string.keyPassword));
    }

    public void removeDeviceIdentification() {
        json.remove(Preferences.getStringRessource(R.string.keyDeviceId));
        json.remove(Preferences.getStringRessource(R.string.keyClientId));
    }

    public void remove(String key) {
        json.remove(key);
    }

}
