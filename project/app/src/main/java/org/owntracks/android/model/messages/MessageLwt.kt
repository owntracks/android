package org.owntracks.android.model.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(MessageLwt.TYPE)
class MessageLwt(
    private val messageWithCreatedAtImpl: MessageWithCreatedAt = MessageCreatedAtNow(RealClock())
) : MessageBase(), MessageWithId, MessageWithCreatedAt by messageWithCreatedAtImpl {
  override var messageId: MessageId = ZeroMessageId

  @SerialName("tst") var timestamp: Long = messageWithCreatedAtImpl.createdAt.epochSecond

  override fun toString(): String = "[MessageLwt]"

  companion object {
    const val TYPE = "lwt"
  }
}
