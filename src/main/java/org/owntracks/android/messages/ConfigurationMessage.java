package org.owntracks.android.messages;

import android.preference.PreferenceFragment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.EnumSet;

import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.support.Preferences;

public class ConfigurationMessage extends Message{
    private static final String TAG = "ConfigurationMessage";

    private JSONObject json;
    public static enum Includes {CONNECTION, CREDENTIALS, IDENTIFICATION, WAYPOINTS}

    public ConfigurationMessage(JSONObject json){
        super();
        this.json=json;
    }

    public ConfigurationMessage() {
        super();

        this.json = Preferences.toJSONObject();
    }

    public String toString() {return toJSONObject().toString(); }
    public JSONObject toJSONObject() {
        return json;
    }

}
