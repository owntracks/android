package st.alr.mqttitude.support;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class StringifiedJSONObject extends JSONObject {

    public StringifiedJSONObject(String str) throws JSONException {
        super(str);
    }
    public StringifiedJSONObject() {
        super();
    }

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

    public StringifiedJSONObject putAll(StringifiedJSONObject o) throws JSONException{
            for(int i = 0 ; i < o.names().length(); i++)
                this.put((String)o.names().get(i), o.names().get(i));
         return this;
    }

    // Created for testing purpose only
    public static boolean compareJSONObjs(JSONObject js1, JSONObject js2) throws JSONException {

        if (js1 == null || js2 == null) {
            return (js1 == js2);
        }

        int i=0;
        String key;

        JSONArray js1Names=js1.names();
        JSONArray js2Names=js2.names();


        for (i=0; i<js1Names.length(); i++) {
            key=js1Names.get(i).toString();

            if(!js1.get(key).equals(js2.get(key)))
                Log.v("StringifiedJSONObject.compareJSONObjs", "Differ: " + key + " " + js1.get(key) + "!=" + js2.get(key));
        }

        return true;
    }

}
