package org.owntracks.android.model.messages

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageWaypoint.TYPE)
class MessageWaypoint(@Transient private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId {
  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("_id")
  override var messageId: MessageId = messageWithId.messageId
  @SerialName("desc") var description: String? = null

  @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("lon") var longitude = 0.0

  @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("lat") var latitude = 0.0

  @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("tst") var timestamp: Long = 0

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
