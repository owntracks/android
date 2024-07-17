package org.owntracks.android.test

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource

/**
 * A shim around a [androidx.test.espresso.idling.CountingIdlingResource] that lets us provide a
 * no-op version in release mode. This is the wrapper implementation
 *
 * @param debugCounting unused
 * @param name Name of the idling resource
 */
class CountingIdlingResourceShim(name: String, debugCounting: Boolean) : IdlingResource {
  private val countingIdlingResource = CountingIdlingResource(name, debugCounting)

  override fun getName(): String = countingIdlingResource.name

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    countingIdlingResource.registerIdleTransitionCallback(callback)
  }

  override fun isIdleNow(): Boolean = countingIdlingResource.isIdleNow

  fun increment() {
    countingIdlingResource.increment()
  }

  fun decrement() {
    countingIdlingResource.decrement()
  }
}
