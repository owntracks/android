package org.owntracks.android.model

import kotlinx.datetime.Instant
import org.owntracks.android.model.messages.Clock

class FakeFixedClock(fakeTime: Instant = Instant.fromEpochMilliseconds(25123)) : Clock {
  override val time: Instant = fakeTime
}
