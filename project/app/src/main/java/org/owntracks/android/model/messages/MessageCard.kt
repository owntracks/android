package org.owntracks.android.model.messages

import androidx.databinding.Bindable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName(MessageCard.TYPE)
class MessageCard(@Transient private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId {
  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("_id")
  override var messageId: MessageId = messageWithId.messageId
  @get:Bindable @SerialName("name") var name: String? = null

  @SerialName("face") var face: String? = null

  @SerialName("tid") var trackerId: String? = null

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  override fun toString(): String = "[MessageCard name=$name]"

  companion object {
    const val BASETOPIC_SUFFIX = "/info"
    const val TYPE = "card"
  }
}
