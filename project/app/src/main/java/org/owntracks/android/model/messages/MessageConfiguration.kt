package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.*
import org.owntracks.android.support.MessageWaypointCollection
import java.util.*

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

    // TID would not be included in map for load otherwise
    @JsonInclude(JsonInclude.Include.NON_NULL)
    fun setTid(tid: String) {
        set("tid", tid)
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

    @JsonIgnore
    fun removeKey(key: String?) {
        map.remove(key)
    }

    companion object {
        const val TYPE = "configuration"
    }
}