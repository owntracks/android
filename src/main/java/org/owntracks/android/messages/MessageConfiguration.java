package org.owntracks.android.messages;

import android.databinding.Bindable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.owntracks.android.db.Waypoint;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageConfiguration extends MessageBase{
    private static final String BASETOPIC_SUFFIX = "/cmd";

    private Map<String,Object> map = new HashMap<String,Object>();



    @JsonProperty
    private List<MessageWaypoint> waypoints;

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processMessage(this);
    }

    public List<MessageWaypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<MessageWaypoint> waypoints) {
        this.waypoints = waypoints;
    }

    // To reduce maintenance effort, the configuration object can have an arbitrary number of key/value attributes for preferences keys and values
    @JsonAnyGetter
    public Map<String,Object> any() {
        return map;
    }

    @JsonAnySetter
    public void set(String key, Object value) {
        map.put(key, value);
    }
    public Object get(String key) {
        return map.get(key);
    }

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


    public Set<String> getKeys() {
        return map.keySet();
    }

    public boolean hasWaypoints() {
        return waypoints != null && waypoints.size() > 0;
    }
}
