package org.owntracks.android.messages;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.EnumSet;

import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.support.Preferences;

public class ConfigurationMessage extends Message{
    private JSONObject json;
    public static enum Includes {CONNECTION, CREDENTIALS, IDENTIFICATION, WAYPOINTS}

    public ConfigurationMessage(JSONObject json){
        super();
        this.json=json;
    }

    public ConfigurationMessage(EnumSet<Includes> includes) {
        super();

        json = Preferences.toJSONObject();

        if(!includes.contains(Includes.CONNECTION))
            stripConnection();

        if(!includes.contains(Includes.CREDENTIALS))
            stripCredentials();

        if(!includes.contains(Includes.IDENTIFICATION))
            stripDeviceIdentification();

        if(includes.contains(Includes.WAYPOINTS))
            try {
                json.put("waypoints", getWaypointJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }


    private JSONObject getPreferencesJson() {
        return Preferences.toJSONObject();
    }
    private JSONArray getWaypointJson() {

        JSONArray waypoints = new JSONArray();
        for(Waypoint waypoint : App.getWaypointDao().loadAll()) {
            WaypointMessage wpM = new WaypointMessage(waypoint);
            JSONObject wp = wpM.toJSONObject();
            try { wp.put("shared", waypoint.getShared()); } catch (JSONException e) { }
            waypoints.put(wp);
        }
        return waypoints;
    }

    public String toString() {return toJSONObject().toString(); }
    public JSONObject toJSONObject() {
        return json;
    }

    public ConfigurationMessage stripCredentials() {
        json.remove(Preferences.getStringRessource(R.string.keyUsername));
        json.remove(Preferences.getStringRessource(R.string.keyPassword));
        return this;
    }

    public ConfigurationMessage stripDeviceIdentification() {
        json.remove(Preferences.getStringRessource(R.string.keyDeviceId));
        json.remove(Preferences.getStringRessource(R.string.keyClientId));
        json.remove(Preferences.getStringRessource(R.string.keyTrackerId));
        return this;
    }

    public ConfigurationMessage stripConnection() {
        json.remove(Preferences.getStringRessource(R.string.keyHost));
        json.remove(Preferences.getStringRessource(R.string.keyPort));
        json.remove(Preferences.getStringRessource(R.string.keyAuth));
        json.remove(Preferences.getStringRessource(R.string.keyTls));
        json.remove(Preferences.getStringRessource(R.string.keyTlsCrtPath));
        json.remove(Preferences.getStringRessource(R.string.keyConnectionAdvancedMode));
        json.remove(Preferences.getStringRessource(R.string.keyCleanSession));
        return this;
    }

    public ConfigurationMessage stripWaypoints() {
        json.remove("waypoints");
        return this;
    }

    public void remove(String key) {
        json.remove(key);
    }



}
