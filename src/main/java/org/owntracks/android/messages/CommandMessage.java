package org.owntracks.android.messages;

import org.json.JSONException;
import org.json.JSONObject;

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

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("_type", "cmd").put("action", this.action);

        }catch (JSONException e) {
        }

        return json;
    }

}
