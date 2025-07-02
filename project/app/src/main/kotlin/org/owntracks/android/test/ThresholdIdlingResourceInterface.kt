package org.owntracks.android.test

import androidx.test.espresso.IdlingResource

/**
 * An interface for an idling resource that tracks a counter and becomes idle when the counter
 * reaches a specified threshold.
 *
 * Implementations of this interface can be used with Espresso to synchronize tests with
 * asynchronous operations that involve a counter, such as network requests or database operations.
 *
 * The counter can be incremented and decremented using the `increment()` and `decrement()` methods.
 * The threshold value can be set using the `setThreshold()` method.
 *
 * The `isIdleNow()` method returns `true` if the counter is equal to the threshold, indicating that
 * the resource is idle. Otherwise, it returns `false`.
 */
interface ThresholdIdlingResourceInterface : IdlingResource {
  fun increment()

  fun decrement()

  fun set(value: Int)

  var threshold: Int
}
