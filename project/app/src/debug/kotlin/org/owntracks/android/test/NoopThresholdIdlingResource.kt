package org.owntracks.android.test

import androidx.test.espresso.IdlingResource

/**
 * A no-op implementation of [ThresholdIdlingResourceInterface] that always reports as idle.
 *
 * It implements all the methods of `ThresholdIdlingResourceInterface` but does not actually track
 * any idle state. `isIdleNow()` always returns `true`, and `increment()` and `decrement()` have no
 * effect.
 */
class NoopThresholdIdlingResource(override var threshold: Int = 0) :
    ThresholdIdlingResourceInterface {
  override fun increment() {}

  override fun decrement() {}

  override fun set(value: Int) {}

  override fun getName(): String = ""

  override fun isIdleNow(): Boolean = true

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {}
}
