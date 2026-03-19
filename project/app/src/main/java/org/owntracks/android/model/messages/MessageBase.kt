package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.IOException
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "_type",
    defaultImpl = MessageUnknown::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = MessageLocation::class, name = MessageLocation.TYPE),
    JsonSubTypes.Type(value = MessageTransition::class, name = MessageTransition.TYPE),
    JsonSubTypes.Type(value = MessageCard::class, name = MessageCard.TYPE),
    JsonSubTypes.Type(value = MessageCmd::class, name = MessageCmd.TYPE),
    JsonSubTypes.Type(value = MessageConfiguration::class, name = MessageConfiguration.TYPE),
    JsonSubTypes.Type(value = MessageEncrypted::class, name = MessageEncrypted.TYPE),
    JsonSubTypes.Type(value = MessageWaypoint::class, name = MessageWaypoint.TYPE),
    JsonSubTypes.Type(value = MessageWaypoints::class, name = MessageWaypoints.TYPE),
    JsonSubTypes.Type(value = MessageLwt::class, name = MessageLwt.TYPE),
    JsonSubTypes.Type(value = MessageStatus::class, name = MessageStatus.TYPE))
@JsonPropertyOrder(alphabetic = true)
abstract class MessageBase : MessageWithId {
  @JsonIgnore open val numberOfRetries: Int = 10

  @JsonIgnore
  open var topic: String = ""
    set(value) {
      field = value
      topicBase = getBaseTopic(value) // Normalized topic for all message types
    }

  @JsonProperty("topic")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @set:JsonIgnore
  var visibleTopic: String = ""

  @JsonIgnore private var topicBase: String? = null

  /**
   * We only add the `topic` attribute if we're publishing over HTTP, so rely on an HTTP transport
   * explicitly setting it by calling this.
   */
  fun setTopicVisible() {
    visibleTopic = topic
  }

  /**
   * Gets the contact identifier for this message, based on the message topic
   *
   * @return
   */
  @JsonIgnore fun getContactId(): String = getBaseTopic(topic)

  @get:JsonIgnore @set:JsonIgnore @JsonIgnore var modeId = ConnectionMode.MQTT

  @get:JsonIgnore @set:JsonIgnore @JsonIgnore var qos = 0

  @get:JsonIgnore @set:JsonIgnore @JsonIgnore var retained = false

  @get:JsonIgnore
  open val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  // Called after deserialization to check if all required attributes are set or not.
  // The message is discarded if false is returned.
  @JsonIgnore open fun isValidMessage(): Boolean = true

  @JsonIgnore
  private fun getBaseTopic(topic: String): String {
    return if (topic.endsWith(baseTopicSuffix)) {
      topic.substring(0, topic.length - baseTopicSuffix.length)
    } else {
      topic
    }
  }

  @JsonIgnore abstract override fun toString(): String

  open fun annotateFromPreferences(preferences: Preferences) {}

  @Throws(IOException::class)
  open fun toJsonBytes(parser: Parser): ByteArray {
    return parser.toJsonBytes(this)
  }

  @Throws(IOException::class)
  open fun toJson(parser: Parser): String? {
    return parser.toJson(this)
  }

  companion object {
    const val TYPE = "base"
    const val BASETOPIC_SUFFIX = ""
  }
}
