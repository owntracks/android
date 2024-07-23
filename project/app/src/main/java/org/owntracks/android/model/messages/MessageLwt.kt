package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonProperty

class MessageLwt(
    private val messageWithCreatedAtImpl: MessageWithCreatedAt = MessageCreatedAtNow(RealClock())
) : MessageBase(), MessageWithId, MessageWithCreatedAt by messageWithCreatedAtImpl {
  override var messageId: MessageId = ZeroMessageId

  @JsonProperty("tst") var timestamp: Long = messageWithCreatedAtImpl.createdAt.epochSecond

  override fun toString(): String = "[MessageLwt]"

  companion object {
    const val TYPE = "lwt"
  }
}
