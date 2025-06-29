package org.owntracks.android.testutils.idlingresources

import androidx.test.espresso.IdlingResource
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.test.IdlingResourceWithData
import timber.log.Timber

/**
 * Idling resource that tracks data
 *
 * @param T
 * @property resourceName
 * @constructor Create empty Idling resource with data
 */
class IdlingResourceWithDataImpl<T : MessageBase>(
    private val resourceName: String,
    private val comparator: Comparator<in T>
) : IdlingResource, IdlingResourceWithData<T> {
  private var callback: IdlingResource.ResourceCallback? = null
  private val sent = mutableListOf<T>()
  private val received = mutableListOf<T>()

  override fun getName(): String = this.resourceName

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }

  override fun isIdleNow(): Boolean = sent.isEmpty() && received.isEmpty()

  override fun add(thing: T) {
    synchronized(sent) {
      synchronized(received) {
        Timber.v("Waiting for return for $thing")
        sent.add(thing)
        reconcile()
      }
    }
  }

  override fun remove(thing: T) {
    synchronized(sent) {
      synchronized(received) {
        Timber.v("Received return for $thing")
        received.add(thing)
        reconcile()
      }
    }
  }

  override fun reconcile() {
    Timber.v(
        "Contents: sent=${sent.joinToString(",") { it.messageId }}, received=${received.joinToString(","){it.messageId}}")
    sent.intersectByComparator(received.toSet(), comparator).let { sentToRemove ->
      received.intersectByComparator((sent.toSet()), comparator).let { receivedToRemove ->
        sent.removeAll(sentToRemove).also {
          Timber.v("Removed $sentToRemove from sent. Success = $it")
        }
        received.removeAll(receivedToRemove).also {
          Timber.v("Removed $receivedToRemove from received. Success = $it")
        }
      }
    }
    if (sent.isEmpty() && received.isEmpty()) {
      Timber.v("$name Empty. Idling.")
      callback?.onTransitionToIdle()
    }
  }

  private fun Collection<T>.intersectByComparator(
      another: Collection<T>,
      comparator: Comparator<in T>
  ): Set<T> {
    return this.filter { a -> another.any { b -> comparator.compare(a, b) == 0 } }.toSet()
  }
}
