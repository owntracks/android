package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.datetime.Instant
import org.owntracks.android.preferences.Preferences

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageWaypoint : MessageBase() {
    @JsonProperty("desc")
    var description: String? = null

    @JsonProperty("lon")
    var longitude = 0.0

    @JsonProperty("lat")
    var latitude = 0.0

    @JsonProperty("tst")
    var timestamp: Long = 0

    // Optional types for optional values
    @JsonProperty("rad")
    var radius: Int? = null

    override fun isValidMessage(): Boolean {
        return super.isValidMessage() && description != null
    }

    override fun toString(): String = "[MessageWaypoint ts=${Instant.fromEpochSeconds(timestamp)},description=$description,lat=$latitude,lon=$longitude,rad=$radius]"

    override fun addMqttPreferences(preferences: Preferences) {
        topic = preferences.pubTopicWaypoint
        qos = preferences.pubQosWaypoints.value
        retained = preferences.pubRetainWaypoints
    }

    override val baseTopicSuffix: String
        get() = BASETOPIC_SUFFIX

    companion object {
        const val TYPE = "waypoint"
        private const val BASETOPIC_SUFFIX = "/event"
    }
}
