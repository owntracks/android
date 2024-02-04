package org.owntracks.android.support

import androidx.test.espresso.IdlingResource
import timber.log.Timber

/**
 * Idling resource that tracks data
 *
 * @param T
 * @constructor Create empty Idling resource with data
 * @property resourceName
 */
class IdlingResourceWithData<T>(
    private val resourceName: String,
    private val comparator: Comparator<T>? = null
) : IdlingResource {
  private var callback: IdlingResource.ResourceCallback? = null
  private val data = mutableListOf<T>()

  override fun getName(): String = this.resourceName

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }

  override fun isIdleNow(): Boolean = data.isEmpty()

  fun add(thing: T) {
    data.add(thing)
    Timber.v("Waiting for return for $thing")
  }

  fun remove(thing: T) {
    data.removeIf { comparator?.compare(it, thing) == 0 }
    val removed = data.remove(thing)
    Timber.v("$name Got return for $thing. Removed=$removed")
    if (data.isEmpty()) {
      Timber.v("$name Empty. Idling.")
      callback?.onTransitionToIdle()
    }
  }
}
