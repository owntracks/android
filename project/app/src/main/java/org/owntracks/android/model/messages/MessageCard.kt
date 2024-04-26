package org.owntracks.android.model.messages

import androidx.databinding.Bindable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageCard(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  @get:Bindable var name: String? = null

  @set:JsonSetter var face: String? = null

  @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("tid") var trackerId: String? = null

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  override fun toString(): String = "[MessageCard name=$name]"

  companion object {
    const val BASETOPIC_SUFFIX = "/info"
    const val TYPE = "card"
  }
}
