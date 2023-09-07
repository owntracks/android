package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageWaypoints : MessageBase() {
    var waypoints: MessageWaypointCollection? = null
    override fun addMqttPreferences(preferences: Preferences) {
        topic = preferences.pubTopicWaypoints
        qos = preferences.pubQosWaypoints.value
        retained = preferences.pubRetainWaypoints
    }

    companion object {
        const val TYPE = "waypoints"
    }
}
