package org.owntracks.android.messages;

import org.json.JSONObject;
import org.owntracks.android.support.Preferences;

public class ConfigurationMessage extends Message{
    private static final String TAG = "ConfigurationMessage";

    private JSONObject json;

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
