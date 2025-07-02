package org.owntracks.android.test

import androidx.test.espresso.IdlingResource
import org.owntracks.android.model.messages.MessageBase

/**
 * Idling resource that tracks data. Noop implementation
 *
 * @param T
 * @property resourceName
 * @constructor Create empty Idling resource with data
 */
open class IdlingResourceWithData<T : MessageBase>(
    private val resourceName: String,
    @Suppress("unused_parameter") comparator: Comparator<in T>
) : IdlingResource {
  private var callback: IdlingResource.ResourceCallback? = null
  private val sent = mutableListOf<T>()
  private val received = mutableListOf<T>()

  override fun getName(): String = this.resourceName

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }

  override fun isIdleNow(): Boolean = sent.isEmpty() && received.isEmpty()

  open fun add(thing: T) {}

  open fun remove(thing: T) {}

  open fun reconcile() {}
}
