package org.owntracks.android.test

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback

/**
 * Simple idling resource. No-op version for release
 *
 * @param initialIdlingState
 * @property resourceName
 * @constructor
 */
class SimpleIdlingResource(
    private val resourceName: String,
    @Suppress("unused_parameter") initialIdlingState: Boolean
) : IdlingResource {

  override fun getName(): String = resourceName

  override fun isIdleNow(): Boolean = true

  override fun registerIdleTransitionCallback(callback: ResourceCallback) {
    // No-op
  }

  fun setIdleState(@Suppress("unused_parameter") isIdleNow: Boolean) {
    // No-op
  }
}
