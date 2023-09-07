package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.TreeMap
import org.owntracks.android.support.MessageWaypointCollection

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
class MessageConfiguration : MessageBase() {
    private val map: MutableMap<String, Any?> = TreeMap()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var waypoints: MessageWaypointCollection = MessageWaypointCollection()

    @JsonAnyGetter
    @JsonPropertyOrder(alphabetic = true)
    fun any(): Map<String, Any?> {
        return map
    }

    @JsonAnySetter
    operator fun set(key: String, value: Any?) {
        map[key] = value
    }

    @JsonIgnore
    operator fun get(key: String?): Any? {
        return map[key]
    }

    @JsonIgnore
    fun containsKey(key: String?): Boolean {
        return map.containsKey(key)
    }

    @get:JsonIgnore
    val keys: Set<String>
        get() = map.keys

    companion object {
        const val TYPE = "configuration"
    }
}
