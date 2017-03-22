package org.owntracks.android.messages;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
public class MessageConfiguration extends MessageBase{
    static final String TYPE = "configuration";
    private static final String BASETOPIC_SUFFIX = "/cmd";
    private Map<String,Object> map = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MessageWaypointCollection waypoints;

    @Override
    public void processIncomingMessage(IncomingMessageProcessor handler) {
        handler.processIncomingMessage(this);
    }

    public MessageWaypointCollection getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(MessageWaypointCollection waypoints) {
        this.waypoints = waypoints;
    }

    @JsonAnyGetter
    public Map<String,Object> any() {
        Timber.v("getting map. length: %s", map.size());
        return map;
    }

    @JsonAnySetter
    public void set(String key, Object value) {
        if(value instanceof String && "".equals(value))
            return;
        Timber.v("import key:%s, value:%s", key, value);

        map.put(key, value);
    }

    // TID would not be included in map for import otherwise
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public void setTid(String tid) {
        set(Preferences.Keys.TRACKER_ID, tid);
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
    @JsonIgnore
    public void processOutgoingMessage(OutgoingMessageProcessor handler) {
        handler.processOutgoingMessage(this);
    }

    @Override
    @JsonIgnore
    public String getBaseTopicSuffix() {
        return null;
    }


    @JsonIgnore
    public Set<String> getKeys() {
        return map.keySet();
    }

    @JsonIgnore
    public void removeKey(String key) {
        map.remove(key);
    }

    @JsonIgnore
    public boolean hasWaypoints() {
        return waypoints != null && waypoints.size() > 0;
    }

}
