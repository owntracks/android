package org.owntracks.android.testutils.idlingresources

import androidx.test.espresso.IdlingResource
import timber.log.Timber

class LatchingIdlingResourceWithData(private val name: String) : IdlingResource {
  private var callback: IdlingResource.ResourceCallback? = null
  private var latched = false
  var data: String? = null

  fun latch(data: String) {
    Timber.d("Latching with $data")
    latched = true
    this.data = data
  }

  fun unlatch() {
    latched = false
    callback?.onTransitionToIdle()
  }

  override fun getName(): String = name

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }

  override fun isIdleNow(): Boolean = !latched.also { Timber.v("Is latched? $latched") }
}
