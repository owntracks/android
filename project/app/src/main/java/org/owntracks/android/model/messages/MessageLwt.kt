@file:UseSerializers(InstantEpochSecondsSerializer::class)

package org.owntracks.android.model.messages

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

@Serializable
@SerialName(MessageLwt.TYPE)
class MessageLwt(
    @Transient
    private val messageWithCreatedAtImpl: MessageWithCreatedAt = MessageCreatedAtNow(RealClock())
) : MessageBase(), MessageWithId, MessageWithCreatedAt {
  override var messageId: MessageId = ZeroMessageId

  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("created_at")
  override var createdAt:
      @kotlinx.serialization.Serializable(with = InstantEpochSecondsSerializer::class)
      Instant =
      messageWithCreatedAtImpl.createdAt

  @SerialName("tst") var timestamp: Long = messageWithCreatedAtImpl.createdAt.epochSeconds

  override fun toString(): String = "[MessageLwt]"

  companion object {
    const val TYPE = "lwt"
  }
}
