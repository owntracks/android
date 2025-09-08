package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.owntracks.android.model.CommandAction
import org.owntracks.android.preferences.Preferences

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageCmd(private val messageWithId: MessageWithId = MessageWithRandomId()) :
    MessageBase(), MessageWithId by messageWithId {
  var action: CommandAction? = null
  var waypoints: MessageWaypoints? = null
  var configuration: MessageConfiguration? = null

  override fun isValidMessage(): Boolean {
    return super.isValidMessage() && action != null
  }

  override fun toString(): String = "[MessageCmd action=$action]"

  override fun annotateFromPreferences(preferences: Preferences) {}

  override val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  companion object {
    const val TYPE = "cmd"
    private const val BASETOPIC_SUFFIX = "/cmd"
  }
}
