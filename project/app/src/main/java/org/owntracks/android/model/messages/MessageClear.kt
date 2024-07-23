package org.owntracks.android.model.messages

import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.Preferences

class MessageClear : MessageBase(), MessageWithId {
  override var messageId: MessageId = ZeroMessageId

  override fun toString(): String = "[MessageClear]"

  override fun addMqttPreferences(preferences: Preferences) {
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
