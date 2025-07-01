package org.owntracks.android.model.messages

import java.time.Instant
import kotlinx.serialization.SerialName

interface MessageWithCreatedAt {
  @get:SerialName("created_at") var createdAt: Instant
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
