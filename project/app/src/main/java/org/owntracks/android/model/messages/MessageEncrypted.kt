package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(MessageEncrypted.TYPE)
class MessageEncrypted(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  var data: String = ""

  companion object {
    const val TYPE = "encrypted"
  }

  override fun toString(): String = "[MessageEncrypted]"
}
