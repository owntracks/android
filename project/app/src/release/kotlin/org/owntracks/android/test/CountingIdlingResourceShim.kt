package org.owntracks.android.test

import androidx.test.espresso.IdlingResource

/**
 * A shim around a [androidx.test.espresso.idling.CountingIdlingResource] that lets us provide a
 * no-op version in release mode. This is the no-op implementation.
 *
 * @param debugCounting unused
 * @constructor
 * @property name Name of the idling resource
 */
class CountingIdlingResourceShim(
    private val name: String,
    @Suppress("unused_parameter") debugCounting: Boolean
) : IdlingResource {

  override fun getName(): String = name

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    // No-op
  }

  override fun isIdleNow(): Boolean = true

  fun increment() {
    // No-op
  }

  fun decrement() {
    // No-op
  }
}
