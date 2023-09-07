package org.owntracks.android.model.messages

import androidx.databinding.BaseObservable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.IOException
import kotlin.random.Random
import okhttp3.internal.toHexString
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.support.Parser

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "_type",
    defaultImpl = MessageUnknown::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = MessageLocation::class, name = MessageLocation.TYPE),
    JsonSubTypes.Type(value = MessageTransition::class, name = MessageTransition.TYPE),
    JsonSubTypes.Type(value = MessageCard::class, name = MessageCard.TYPE),
    JsonSubTypes.Type(value = MessageCmd::class, name = MessageCmd.TYPE),
    JsonSubTypes.Type(value = MessageConfiguration::class, name = MessageConfiguration.TYPE),
    JsonSubTypes.Type(value = MessageEncrypted::class, name = MessageEncrypted.TYPE),
    JsonSubTypes.Type(value = MessageWaypoint::class, name = MessageWaypoint.TYPE),
    JsonSubTypes.Type(value = MessageWaypoints::class, name = MessageWaypoints.TYPE),
    JsonSubTypes.Type(value = MessageLwt::class, name = MessageLwt.TYPE)
)
@JsonPropertyOrder(alphabetic = true)
abstract class MessageBase : BaseObservable() {
    @JsonIgnore
    open val numberOfRetries: Int = 10

    @get:JsonIgnore
    @JsonIgnore
    val messageId = "${System.currentTimeMillis()}-${Random.nextInt(0X1000000).toHexString()}"

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

    @JsonIgnore
    private var topicBase: String? = null

    fun setTopicVisible() {
        visibleTopic = topic
    }

    @get:JsonIgnore
    @set:JsonIgnore
    @JsonIgnore
    var modeId = ConnectionMode.MQTT

    @get:JsonIgnore
    @JsonIgnore
    var isIncoming = false
        private set

    @get:JsonIgnore
    @set:JsonIgnore
    @JsonIgnore
    var qos = 0

    @get:JsonIgnore
    @set:JsonIgnore
    @JsonIgnore
    var retained = false

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("tid")
    open var trackerId: String? = null

    @get:JsonIgnore
    val contactKey: String
        get() {
            if (topicBase != null) return topicBase!!
            return if (trackerId != null) trackerId!! else "NOKEY"
        }

    @JsonIgnore
    fun setIncoming() {
        isIncoming = true
    }

    @get:JsonIgnore
    protected open val baseTopicSuffix: String?
        get() = BASETOPIC_SUFFIX

    // Called after deserialization to check if all required attributes are set or not.
    // The message is discarded if false is returned.
    @JsonIgnore
    open fun isValidMessage(): Boolean = true

    @JsonIgnore
    fun hasTrackerId(): Boolean {
        return trackerId != null
    }

    @JsonIgnore
    private fun getBaseTopic(topic: String): String {
        return if (baseTopicSuffix != null && topic.endsWith(baseTopicSuffix!!)) {
            topic.substring(0, topic.length - baseTopicSuffix!!.length)
        } else {
            topic
        }
    }

    @JsonIgnore
    override fun toString(): String {
        return String.format("${this.javaClass.name} id=$messageId")
    }

    open fun addMqttPreferences(preferences: Preferences) {}

    @Throws(IOException::class)
    open fun toJsonBytes(parser: Parser): ByteArray? {
        return parser.toJsonBytes(this)
    }

    @Throws(IOException::class)
    open fun toJson(parser: Parser): String? {
        return parser.toJson(this)
    }

    companion object {
        const val TYPE = "base"
        val BASETOPIC_SUFFIX = null
    }
}
