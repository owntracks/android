package org.owntracks.android.location.profiles

import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Source of the current [DeviceContext] */
interface DeviceContextProvider {
  /**
   * The current [DeviceContext], kept up to date as the device state changes. The device is only
   * watched while this is being collected, and the first context isn't emitted until every part of
   * it is known, so that profiles aren't matched against a context that isn't real yet.
   */
  val deviceContext: Flow<DeviceContext>
}

/**
 * Emits [fallback] if nothing has been emitted within [timeout], so that one source that never
 * reports can't hold up the others for ever.
 */
internal fun <T> Flow<T>.orFallbackAfter(timeout: Duration, fallback: T): Flow<T> = channelFlow {
  // Checking whether anything's been emitted and sending happen together, so that the fallback
  // can't be sent after a value it lost the race to
  val sending = Mutex()
  var emitted = false
  val fallbackJob = launch {
    delay(timeout)
    sending.withLock {
      if (!emitted) {
        emitted = true
        send(fallback)
      }
    }
  }
  collect {
    sending.withLock {
      emitted = true
      fallbackJob.cancel()
      send(it)
    }
  }
}
