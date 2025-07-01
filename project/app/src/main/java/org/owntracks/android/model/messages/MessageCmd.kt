package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.owntracks.android.model.CommandAction
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageCmd.TYPE)
class MessageCmd(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  var action: CommandAction? = null
  var waypoints: MessageWaypoints? = null
  var configuration: MessageConfiguration? = null

  override fun isValidMessage(): Boolean {
    return super.isValidMessage() && action != null
  }

  override fun toString(): String = "[MessageCmd action=$action]"

  override fun annotateFromPreferences(preferences: Preferences) {
    topic = preferences.receivedCommandsTopic
  }

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  companion object {
    const val TYPE = "cmd"
    private const val BASETOPIC_SUFFIX = "/cmd"
  }
}
