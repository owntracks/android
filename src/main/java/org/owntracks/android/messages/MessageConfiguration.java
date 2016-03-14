package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.OutgoingMessageProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY) // Don't serialize attributes with empty values
public class MessageConfiguration extends MessageBase{
    private static final String BASETOPIC_SUFFIX = "/cmd";

    private Map<String,Object> map = new HashMap<>();


    @JsonProperty
    private MessageWaypointCollection waypoints;

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    public MessageWaypointCollection getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(MessageWaypointCollection waypoints) {
        this.waypoints = waypoints;
    }

    // To reduce maintenance effort, the configuration object can have an arbitrary number of key/value attributes for preferences keys and values
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
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processMessage(this);
    }

    @Override
    public String getBaseTopicSuffix() {
        return null;
    }


    @JsonIgnore
    public Set<String> getKeys() {
        return map.keySet();
    }

    @JsonIgnore
    public boolean hasWaypoints() {
        return waypoints != null && waypoints.size() > 0;
    }


}
