package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection

@Serializable
@SerialName(MessageWaypoints.TYPE)
class MessageWaypoints(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  var waypoints: MessageWaypointCollection? = null

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
