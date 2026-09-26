package org.owntracks.android.model.messages

import kotlin.time.ExperimentalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.model.FakeFixedClock

@OptIn(ExperimentalTime::class)
class MessageLwtTest {
  @Test
  fun `an lwt message has a timestamp and created_at which are equal`() {
    val messageLwt = MessageLwt(MessageCreatedAtNow(FakeFixedClock()))
    assertEquals(25, messageLwt.timestamp)
    assert(messageLwt.timestamp == messageLwt.createdAt.epochSeconds)
  }
}
