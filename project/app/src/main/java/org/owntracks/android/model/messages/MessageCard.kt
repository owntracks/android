package org.owntracks.android.model.messages

import androidx.databinding.Bindable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(MessageCard.TYPE)
class MessageCard(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
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
