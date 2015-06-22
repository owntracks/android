package org.owntracks.android.messages;

import org.json.JSONException;
import org.json.JSONObject;

public class CardMessage extends Message {
    private static final String TAG = "CardMessage";

    private String name;
    private String face;
	public CardMessage(JSONObject json) {
        super();
        try{ this.name = json.getString("name"); } catch (JSONException e) {};
        try{ this.face = json.getString("face"); } catch (JSONException e) {};
    }

    public String getName() {
        return name;
    }

    public String getFace() {
        return face;
    }
}
