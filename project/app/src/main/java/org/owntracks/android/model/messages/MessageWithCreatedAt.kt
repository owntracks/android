package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonProperty

interface MessageWithCreatedAt {
    @get:JsonProperty("created_at")
    val createdAt: Long
}

class MessageCreatedAtNow(clock: Clock) : MessageWithCreatedAt {
    override val createdAt: Long = clock.time
}

interface Clock {
    val time: Long
}

class RealClock : Clock {
    override val time: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
}
