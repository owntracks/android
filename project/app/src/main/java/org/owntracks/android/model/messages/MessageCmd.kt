package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.model.CommandAction
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageCmd.TYPE)
class MessageCmd(@Transient private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId {
  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("_id")
  override var messageId: MessageId = messageWithId.messageId
  var action: CommandAction? = null
  var waypoints: MessageWaypoints? = null
  var configuration: MessageConfiguration? = null

  override fun isValidMessage(): Boolean {
    return super.isValidMessage() && action != null
  }

  override fun toString(): String = "[MessageCmd action=$action]"

  override fun annotateFromPreferences(preferences: Preferences) {}

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  companion object {
    const val TYPE = "cmd"
    private const val BASETOPIC_SUFFIX = "/cmd"
  }
}
