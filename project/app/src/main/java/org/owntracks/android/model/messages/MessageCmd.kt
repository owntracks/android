package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.owntracks.android.model.CommandAction
import org.owntracks.android.support.Preferences

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageCmd : MessageBase() {
    var action: CommandAction? = null
    var waypoints: MessageWaypoints? = null
    var configuration: MessageConfiguration? = null

    public override fun isValidMessage(): Boolean {
        return super.isValidMessage() && action != null
    }

    override fun addMqttPreferences(preferences: Preferences) {
        topic = preferences.pubTopicCommands
    }

    @JsonIgnore
    // Full topic is needed instead of the normalized base topic to verify if the message arrived on the correct topic
    override var topic: String = ""


    companion object {
        const val TYPE = "cmd"
        private const val BASETOPIC_SUFFIX = "/cmd"
    }
}