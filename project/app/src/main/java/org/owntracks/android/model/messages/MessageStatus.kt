package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.BuildConfig
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageStatus.TYPE)
class MessageStatus(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {

  var android: AddMessageStatus? = null

  @Transient override fun toString(): String = "[MessageStatus android=${android}]"

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

@Serializable
class AddMessageStatus {
  @SerialName("wifi") var wifistate = 0

  @SerialName("ps") var powerSave = 0

  @SerialName("bo") var batteryOptimizations = 0

  @SerialName("hib") var appHibernation = 0

  @SerialName("loc") var locationPermission = 0

  @SerialName("version") var version = BuildConfig.VERSION_CODE

  @SerialName("flavour") var flavour = BuildConfig.FLAVOR
}
