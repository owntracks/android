package org.owntracks.android.support;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class StringifiedJSONArray extends JSONArray{
    @Override
    public StringifiedJSONObject getJSONObject(int index) throws JSONException {
        return (StringifiedJSONObject)super.getJSONObject(index);
    }

    public StringifiedJSONArray put(int index, StringifiedJSONObject value) throws JSONException {
        return (StringifiedJSONArray)super.put(index, value);
    }
}
