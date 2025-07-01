package org.owntracks.android.model.messages

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageWaypoint.TYPE)
class MessageWaypoint(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  @SerialName("desc") var description: String? = null

  @SerialName("lon") var longitude = 0.0

  @SerialName("lat") var latitude = 0.0

  @SerialName("tst") var timestamp: Long = 0

  // Optional types for optional values
  @SerialName("rad") var radius: Int? = null

  override fun isValidMessage(): Boolean {
    return super.isValidMessage() && description != null
  }

  override fun toString(): String =
      "[MessageWaypoint ts=${Instant.fromEpochSeconds(timestamp)},description=$description,lat=$latitude,lon=$longitude,rad=$radius]"

  override fun annotateFromPreferences(preferences: Preferences) {
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
