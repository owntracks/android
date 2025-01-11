package org.owntracks.android.testutils.idlingresources

import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import timber.log.Timber

/**
 * A working implementation of [ThresholdIdlingResourceInterface] around a
 * [androidx.test.espresso.idling.CountingIdlingResource] that lets us provide a no-op version in
 * release mode. This is the wrapper implementation
 *
 * @param debugCounting unused
 * @param name Name of the idling resource
 */
class TestThresholdIdlingResource(private val name: String) : ThresholdIdlingResourceInterface {

  @Volatile private var resourceCallback: ResourceCallback? = null

  override fun registerIdleTransitionCallback(resourceCallback: ResourceCallback) {
    this.resourceCallback = resourceCallback
  }

  override fun getName(): String = name

  override fun isIdleNow(): Boolean =
      value.get().also { Timber.tag("ARSE").d("Counter idle check. $it") }.run { this == threshold }

  private var value = AtomicInteger(0)

  override fun increment() {
    value.incrementAndGet().also { Timber.tag("ARSE").d("Incremented value to $it") }
  }

  override fun set(value: Int) {
    this.value.set(value).also { Timber.tag("ARSE").d("Set value to $value") }
  }

  override var threshold: Int = 0
    set(value) {
      field = value
      Timber.tag("ARSE").d("Set threshold to $value")
    }

  override fun decrement() {
    value.decrementAndGet().also { Timber.tag("ARSE").d("Decremented value to $it") }
  }
}
