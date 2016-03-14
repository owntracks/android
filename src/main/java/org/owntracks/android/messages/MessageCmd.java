package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageCmd extends MessageBase{
    private static final String BASETOPIC_SUFFIX = "/cmd";
    private String action;

    public static final String ACTION_REPORT_LOCATION = "reportLocation";
    public static final String ACTION_SET_WAYPOINTS = "setWaypoints";
    public static final String ACTION_SET_WAYPOINTS_KEY_WAYPOINTS = "waypoints";

    public static final String ACTION_WAYPOINTS = "waypoints";

    private MessageWaypoints waypoints;
    private Map<String,Object> map = new HashMap<>();

    @JsonAnyGetter
    public Map<String,Object> any() {
        return map;
    }

    @JsonAnySetter
    public void set(String key, Object value) {
        if(value instanceof String && ((String) value).isEmpty())
            return;

        map.put(key, value);
    }

    @JsonIgnore
    public Object get(String key) {
        return map.get(key);
    }

    @JsonIgnore
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }


    @Override
    public String getBaseTopicSuffix() {  return BASETOPIC_SUFFIX; }


    public String getAction() {
        return action;
    }

    @JsonIgnore
    public MessageWaypoints getMessageWaypoints() {
        return waypoints;
    }


    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public boolean isValidMessage() {
        return super.isValidMessage() && (action != null);
    }

}
