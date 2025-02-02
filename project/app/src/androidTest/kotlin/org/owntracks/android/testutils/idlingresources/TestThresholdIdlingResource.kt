package org.owntracks.android.testutils.idlingresources

import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile
import org.owntracks.android.test.ThresholdIdlingResourceInterface

/**
 * A working implementation of [ThresholdIdlingResourceInterface] around a
 * [androidx.test.espresso.idling.CountingIdlingResource] that lets us provide a no-op version in
 * release mode. This is the wrapper implementation
 *
 * @param name Name of the idling resource
 */
class TestThresholdIdlingResource(private val name: String) : ThresholdIdlingResourceInterface {
  @Volatile private var resourceCallback: ResourceCallback? = null

  override fun registerIdleTransitionCallback(resourceCallback: ResourceCallback) {
    this.resourceCallback = resourceCallback
  }

  override fun getName(): String = name

  override fun isIdleNow(): Boolean = value.get().run { this == threshold }

  private var value = AtomicInteger(0)

  override fun increment() {
    value.incrementAndGet().also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
  }

  override fun set(v: Int) {
    value.set(v).also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
  }

  override fun decrement() {
    value.decrementAndGet().also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
  }

  override var threshold: Int = 0
    set(value) = run {
      field = value.also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
    }
}
