package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.support.MessageWaypointCollection

@Serializable
@SerialName(MessageConfiguration.TYPE)
class MessageConfiguration(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  private val map: MutableMap<String, Any?> = mutableMapOf()

  var waypoints: MessageWaypointCollection = MessageWaypointCollection()

  fun any(): Map<String, Any?> {
    return map
  }

  operator fun set(key: String, value: Any?) {
    map[key] = value
  }

  @Transient
  operator fun get(key: String?): Any? {
    return map[key]
  }

  @Transient
  fun containsKey(key: String?): Boolean {
    return map.containsKey(key)
  }

  @get:Transient
  val keys: Set<String>
    get() = map.keys

  companion object {
    const val TYPE = "configuration"
  }

  override fun toString(): String = "[MessageConfiguration]"
}
