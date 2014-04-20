package st.alr.mqttitude.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import st.alr.mqttitude.support.Preferences;

public class ConfigurationMessage {
    public String toString() {return toJSONObject().toString(); }
    public JSONObject toJSONObject() {
        return Preferences.toJSONObject();
    }
}
