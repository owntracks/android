package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName(MessageEncrypted.TYPE)
class MessageEncrypted(
    @Transient private val messageWithId: MessageWithId = MessageWithRandomId()
) : MessageBase(), MessageWithId {
  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("_id")
  override var messageId: MessageId = messageWithId.messageId
  var data: String = ""

  companion object {
    const val TYPE = "encrypted"
  }

  override fun toString(): String = "[MessageEncrypted]"
}
