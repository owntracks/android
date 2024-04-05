package org.owntracks.android.support

import androidx.test.espresso.IdlingResource
import org.owntracks.android.model.messages.MessageBase

/**
 * Idling resource that tracks data. Noop implementation
 *
 * @param T
 * @constructor Create empty Idling resource with data
 * @property resourceName
 */
@Suppress("unused", "EmptyMethod")
class IdlingResourceWithData<T : MessageBase>(
    private val resourceName: String,
    private val comparator: Comparator<in T>
) : IdlingResource {
  private var callback: IdlingResource.ResourceCallback? = null
  private val sent = mutableListOf<T>()
  private val received = mutableListOf<T>()

  override fun getName(): String = this.resourceName

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }

  override fun isIdleNow(): Boolean = sent.isEmpty() && received.isEmpty()

  fun add(thing: T) {
    // No-op
  }

  fun remove(thing: T) {
    // No-op
  }
}
