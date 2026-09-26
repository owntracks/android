package org.owntracks.android.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.owntracks.android.model.messages.Clock

@OptIn(ExperimentalTime::class)
class FakeFixedClock(fakeTime: Instant = Instant.fromEpochMilliseconds(25123)) : Clock {
  override val time: Instant = fakeTime
}
