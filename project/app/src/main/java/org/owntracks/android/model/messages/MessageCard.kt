package org.owntracks.android.model.messages

import androidx.databinding.Bindable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageCard : MessageBase() {
    @get:Bindable
    var name: String? = null

    @set:JsonSetter
    var face: String? = null
    fun hasFace(): Boolean {
        return face != null
    }

    fun hasName(): Boolean {
        return name != null
    }

    public override fun getBaseTopicSuffix(): String {
        return BASETOPIC_SUFFIX
    }

    companion object {
        const val BASETOPIC_SUFFIX = "/info"
        const val TYPE = "card"
    }
}