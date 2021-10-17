package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonProperty

interface MessageWithCreatedAt {
    @get:JsonProperty("created_at")
    val createdAt: Long
}