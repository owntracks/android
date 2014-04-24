package st.alr.mqttitude.support;

import org.json.JSONException;
import org.json.JSONObject;


public class StringifiedJSONObject extends JSONObject {

    public JSONObject put(String name, boolean value) throws JSONException {
        return putEverythingAsString(name, value ? "1" : "0");
    }

    public JSONObject put(String name, double value) throws JSONException {
        return putEverythingAsString(name, value);
    }

    public JSONObject put(String name, int value) throws JSONException {
        return putEverythingAsString(name, value);
    }

    public JSONObject put(String name, long value) throws JSONException {
        return putEverythingAsString(name, value);
    }



    public JSONObject putEverythingAsString(String name, Object value) throws JSONException {
        if (value != null)
            return super.put(name, String.valueOf(value));
        else
            return super.put(name, "");
    }

    public boolean getBoolean(String name) throws JSONException {
        return get(name).equals("1") ? true : false;
    }
}
