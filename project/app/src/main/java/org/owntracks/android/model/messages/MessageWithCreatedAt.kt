package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

interface MessageWithCreatedAt {
  @get:JsonProperty("created_at") var createdAt: Instant
}

class MessageCreatedAtNow(clock: Clock) : MessageWithCreatedAt {
  override var createdAt: Instant = clock.time
}

interface Clock {
  val time: Instant
}

class RealClock : Clock {
  override val time: Instant = Instant.now()
}
