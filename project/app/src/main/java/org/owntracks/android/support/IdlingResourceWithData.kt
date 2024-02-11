package org.owntracks.android.support

import androidx.test.espresso.IdlingResource
import java.util.function.Predicate
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
    private val comparator: Comparator<in T>
) : IdlingResource {
  private var callback: IdlingResource.ResourceCallback? = null
  private val data = mutableListOf<T>()

  override fun getName(): String = this.resourceName

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }

  override fun isIdleNow(): Boolean = data.isEmpty()

  fun add(thing: T) {
    synchronized(data) {
      data.add(thing)
      Timber.v("Waiting for return for $thing")
    }
  }

  fun remove(thing: T) {
    synchronized(data) {
      val removed = data.removeFirst { comparator.compare(it, thing) == 0 }
      Timber.v("$name Got return for $thing. Removed=$removed. Remaining=${data.joinToString(",")}")
      if (data.isEmpty()) {
        Timber.v("$name Empty. Idling.")
        callback?.onTransitionToIdle()
      }
    }
  }

  /**
   * Removes first item in collection that matches the predicate
   *
   * @param filter predicate
   * @return whether or not an item was removed
   */
  private fun <E> MutableCollection<E>.removeFirst(filter: Predicate<in E>): Boolean {
    val iterator = this.iterator()
    while (iterator.hasNext()) {
      val element = iterator.next()
      if (filter.test(element)) {
        iterator.remove()
        return true
      }
    }
    return false
  }
}
