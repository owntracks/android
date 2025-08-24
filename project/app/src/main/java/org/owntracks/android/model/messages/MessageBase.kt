package org.owntracks.android.model.messages

import androidx.databinding.BaseObservable
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode

@Serializable
@Polymorphic
@SerialName("_type")
abstract class MessageBase : BaseObservable(), MessageWithId {
  @Transient open val numberOfRetries: Int = 10

  @Transient
  open var topic: String = ""
    set(value) {
      field = value
      topicBase = getBaseTopic(value) // Normalized topic for all message types
    }

  @SerialName("topic") var visibleTopic: String = ""

  @Transient private var topicBase: String? = null

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
  fun getContactId(): String = getBaseTopic(topic)

  @SerialName("modeId")
  var modeId = ConnectionMode.MQTT

   @SerialName("qos") var qos = 0

   @SerialName("retained") var retained = false


  open val baseTopicSuffix: String
    get() = BASETOPIC_SUFFIX

  // Called after deserialization to check if all required attributes are set or not.
  // The message is discarded if false is returned.
   open fun isValidMessage(): Boolean = true


  private fun getBaseTopic(topic: String): String {
    return if (topic.endsWith(baseTopicSuffix)) {
      topic.substring(0, topic.length - baseTopicSuffix.length)
    } else {
      topic
    }
  }

abstract override fun toString(): String

  open fun annotateFromPreferences(preferences: Preferences) {}
open fun toJsonBytes(parser: Parser): ByteArray {
    return parser.toJsonBytes(this)
  }

  open fun toJson(parser: Parser): String? {
    return parser.toJson(this)
  }

  companion object {
    const val TYPE = "base"
    const val BASETOPIC_SUFFIX = ""
  }
}
