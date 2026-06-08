package org.owntracks.android.model.messages

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection

@Serializable
@SerialName(MessageWaypoints.TYPE)
class MessageWaypoints(
    @Transient private val messageWithId: MessageWithId = MessageWithRandomId()
) : MessageBase(), MessageWithId {
  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("_id")
  override var messageId: MessageId = messageWithId.messageId
  @Contextual var waypoints: MessageWaypointCollection? = null

  override fun toString(): String = "[MessageWaypoints waypoints=${waypoints?.size}]"

  override fun annotateFromPreferences(preferences: Preferences) {
    topic = preferences.pubTopicWaypoints
    qos = preferences.pubQosWaypoints.value
    retained = preferences.pubRetainWaypoints
  }

  companion object {
    const val TYPE = "waypoints"
  }
}
