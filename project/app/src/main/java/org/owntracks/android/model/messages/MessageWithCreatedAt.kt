package org.owntracks.android.model.messages

interface MessageWithCreatedAt {
    val created_at: Long
}

class MessageCreatedAtNow(clock: Clock) : MessageWithCreatedAt {
    override val created_at: Long = clock.time
}

interface Clock {
    val time: Long
}

class RealClock : Clock {
    override val time: Long = System.currentTimeMillis() / 1000L
}

