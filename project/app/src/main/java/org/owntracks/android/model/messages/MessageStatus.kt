package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.datetime.Instant
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageStatus(private val messageWithId: MessageWithId = MessageWithRandomId()) :
  MessageBase(), MessageWithId by messageWithId {
  @JsonIgnore
  override fun toString(): String =
      "[MessageStatus wifi=$wifistate,ps=$powerSave,bo=$batteryOptimizations,hib=$appHibernation,loc=$locationPermission]"

  @JsonProperty("wifi") var wifistate = 0

  @JsonProperty("ps") var powerSave = 0

  @JsonProperty("bo") var batteryOptimizations = 0

  @JsonProperty("hib") var appHibernation = 0

  @JsonProperty("loc") var locationPermission = 0

  override fun addMqttPreferences(preferences: Preferences) {
    topic = preferences.pubTopicStatus
    qos = preferences.pubQosStatus.value
    retained = preferences.pubRetainStatus
  }

  override val baseTopicSuffix: String
    get() = MessageStatus.BASETOPIC_SUFFIX

  companion object {
    const val TYPE = "status"
    private const val BASETOPIC_SUFFIX = "/status"
  }
}
