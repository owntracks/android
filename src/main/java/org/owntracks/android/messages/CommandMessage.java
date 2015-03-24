package org.owntracks.android.messages;

import org.json.JSONException;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.support.StringifiedJSONObject;

import java.util.concurrent.TimeUnit;

public class CommandMessage extends Message {
    String action;
	public CommandMessage(String action) {
        super();
        this.action = action;
    }

	@Override
	public String toString() {
        	return toJSONObject().toString();
	}

    public StringifiedJSONObject toJSONObject() {
        StringifiedJSONObject json = new StringifiedJSONObject();
        try {
            json.put("_type", "cmd").put("action", this.action);

        }catch (JSONException e) {
        }

        return json;
    }

}
