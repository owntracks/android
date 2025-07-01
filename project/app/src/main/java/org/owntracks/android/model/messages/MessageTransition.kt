package org.owntracks.android.model.messages

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageTransition.TYPE)
class MessageTransition(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {

  @Transient
  fun getTransition(): Int =
      when (event) {
        EVENT_ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
        EVENT_LEAVE -> Geofence.GEOFENCE_TRANSITION_EXIT
        else -> 0
      }

  fun setTransition(value: Int) {
    this.event =
        when (value) {
          Geofence.GEOFENCE_TRANSITION_ENTER,
          Geofence.GEOFENCE_TRANSITION_DWELL -> EVENT_ENTER
          Geofence.GEOFENCE_TRANSITION_EXIT -> EVENT_LEAVE
          else -> null
        }
  }

  @SerialName("event") var event: String? = null

  @SerialName("desc") var description: String? = null

  @SerialName("tid") var trackerId: String? = null

  @SerialName("t") var trigger: String? = null

  @SerialName("tst") var timestamp: Long = 0

  @SerialName("wtst") var waypointTimestamp: Long = 0

  @SerialName("acc") var accuracy = 0

  @SerialName("lon") var longitude = 0.0

  @SerialName("lat") var latitude = 0.0

  override fun annotateFromPreferences(preferences: Preferences) {
    topic = preferences.pubTopicEvents
    qos = preferences.pubQosEvents.value
    retained = preferences.pubRetainEvents
  }

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  override fun toString(): String =
      "[MessageTransition ts=${Instant.fromEpochSeconds(timestamp)},event=$event,desc=$description,lon=$longitude,lat=$latitude,acc=$accuracy,trigger=$trigger,trackerId=$trackerId]"

  companion object {
    const val TYPE = "transition"
    const val TRIGGER_CIRCULAR = "c"
    const val TRIGGER_LOCATION = "l"
    private const val BASETOPIC_SUFFIX = "/event"
    private const val EVENT_ENTER = "enter"
    private const val EVENT_LEAVE = "leave"
  }
}
