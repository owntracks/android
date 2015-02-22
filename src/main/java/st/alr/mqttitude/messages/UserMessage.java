package st.alr.mqttitude.messages;

import android.util.Log;

import org.json.JSONException;

import st.alr.mqttitude.support.StringifiedJSONObject;

public class UserMessage {
    private String body;
    private String sender;
    private String title;
    private long time;

    private UserMessage(){
    }

    public String getBody() {
        return body;
    }

    public String getSender() {
        return sender;
    }

    public String getTitle() {
        return title;
    }

    public long getTime() {
        return time;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTime(long time) {
        this.time = time;
    }

	public static UserMessage fromJsonObject(StringifiedJSONObject json) {
		try {
			String type = json.getString("_type");
			if (!type.equals("user"))
				throw new JSONException("wrong type");
		} catch (JSONException e) {
			Log.e("UserMessage",
					"Unable to deserialize UserMessage object from JSON "
							+ json.toString());
			return null;
		}

		UserMessage m = new UserMessage();

		try {
			m.setBody(json.getString("body"));
		} catch (Exception e) {
		    m.setBody("");
        }

		try {
			m.setTitle(json.getString("title"));
		} catch (Exception e) {
            m.setTitle("");
		}

        try {
            m.setSender(json.getString("sender"));
        } catch (Exception e) {
            m.setSender("");
        }

        try {
            m.setTime(json.getLong("time"));
        } catch (Exception e) {
            m.setTime(0);
        }

		return m;
	}
}
