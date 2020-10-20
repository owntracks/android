package org.owntracks.android.model.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.owntracks.android.model.CommandAction;
import org.owntracks.android.support.Preferences;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageCmd extends MessageBase {
    static final String TYPE = "cmd";
    private static final String BASETOPIC_SUFFIX = "/cmd";
    private CommandAction action;

    private MessageWaypoints waypoints;
    private MessageConfiguration configuration;

    @Override
    @NonNull
    public String getBaseTopicSuffix() {
        return BASETOPIC_SUFFIX;
    }

    @Nullable
    public CommandAction getAction() {
        return action;
    }

    public void setAction(CommandAction action) {
        this.action = action;
    }

    @Nullable
    public MessageWaypoints getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(MessageWaypoints m) {
        this.waypoints = m;
    }

    @Nullable
    public MessageConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MessageConfiguration m) {
        this.configuration = m;
    }

    @Override
    public boolean isValidMessage() {
        return super.isValidMessage() && (action != null);
    }

    @Override
    public void addMqttPreferences(Preferences preferences) {
        setTopic(preferences.getPubTopicCommands());
    }

    @Override
    @JsonIgnore
    public void setTopic(String topic) {
        // Full topic is needed instead of the normalized base topic to verify if the message arrived on the correct topic
        this._topic = topic;
    }
}
