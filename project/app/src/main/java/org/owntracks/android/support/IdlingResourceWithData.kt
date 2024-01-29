package org.owntracks.android.support

import androidx.test.espresso.IdlingResource
import timber.log.Timber
import java.util.TreeSet

/**
 * Idling resource that tracks data
 *
 * @param T
 * @constructor Create empty Idling resource with data
 * @property resourceName
 */
class IdlingResourceWithData<T>(private val resourceName: String, comparator: Comparator<T>? = null) : IdlingResource {
  private var callback: IdlingResource.ResourceCallback? = null
  private val data = TreeSet(comparator)

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
    val removed = data.remove(thing)
    Timber.v("Got return for $thing. Removed=$removed")
    if (data.isEmpty()) {
      Timber.v("Empty. Idling.")
      callback?.onTransitionToIdle()
    }
  }
}
