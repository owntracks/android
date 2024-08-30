package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.owntracks.android.preferences.Preferences

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageStatus(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {

  var android: AddMessageStatus? = null

  @JsonIgnore override fun toString(): String = "[MessageStatus android=${android}]"

  override fun annotateFromPreferences(preferences: Preferences) {
    topic = preferences.pubTopicStatus
    qos = preferences.pubQosStatus.value
    retained = preferences.pubRetainStatus
  }

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  companion object {
    const val TYPE = "status"
    private const val BASETOPIC_SUFFIX = "/status"
    const val STATUS_PASS = 0
    const val STATUS_FAIL = 1
    const val STATUS_WIFI_ENABLED = 1
    const val STATUS_WIFI_DISABLED = 0
  }
}

class AddMessageStatus {
  @JsonProperty("wifi") var wifistate = 0

  @JsonProperty("ps") var powerSave = 0

  @JsonProperty("bo") var batteryOptimizations = 0

  @JsonProperty("hib") var appHibernation = 0

  @JsonProperty("loc") var locationPermission = 0
}
