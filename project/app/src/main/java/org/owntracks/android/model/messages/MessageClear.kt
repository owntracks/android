package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.Preferences

@Serializable
@SerialName(MessageClear.TYPE)
class MessageClear : MessageBase(), MessageWithId {
  companion object {
    const val TYPE = "clear"
  }

  override var messageId: MessageId = ZeroMessageId

  override fun toString(): String = "[MessageClear]"

  override fun annotateFromPreferences(preferences: Preferences) {
    retained = true
  }

  // Clear messages are implemented as empty messages
  override fun toJsonBytes(parser: Parser): ByteArray {
    return ByteArray(0)
  }

  override fun toJson(parser: Parser): String {
    return ""
  }
}
