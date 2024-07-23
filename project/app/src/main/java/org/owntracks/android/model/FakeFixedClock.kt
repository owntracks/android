package org.owntracks.android.model

import java.time.Instant
import org.owntracks.android.model.messages.Clock

class FakeFixedClock(fakeTime: Instant = Instant.ofEpochMilli(25123)) : Clock {
  override val time: Instant = fakeTime
}
