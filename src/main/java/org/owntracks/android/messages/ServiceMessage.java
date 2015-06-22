package org.owntracks.android.messages;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.db.Waypoint;

import java.util.concurrent.TimeUnit;

public class ServiceMessage extends Message {
    private static final String TAG = "ServiceMessage";


	public ServiceMessage() {
        super();
    }

	@Override
	public String toString() {
        	return toJSONObject().toString();
	}

    public JSONObject toJSONObject() {
        Log.e(TAG, "toJSONObject is not supported");
        JSONObject json = new JSONObject();
        return json;
    }
}
