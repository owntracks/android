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
 * @param name Name of the idling resource
 */
class TestThresholdIdlingResource(
    private val name: String,
    private val enableLog: Boolean = false
) : ThresholdIdlingResourceInterface {
  init {
    if (enableLog) {
      Timber.d("Creating $name@${hashCode()}")
    }
  }

  @Volatile private var resourceCallback: ResourceCallback? = null

  override fun registerIdleTransitionCallback(resourceCallback: ResourceCallback) {
    if (enableLog) {
      Timber.d("Registering idle transition callback for $name@${this.hashCode()}")
    }
    this.resourceCallback = resourceCallback
  }

  override fun getName(): String = name

  override fun isIdleNow(): Boolean =
      value
          .get()
          .run { this == threshold }
          .also {
            if (enableLog) {
              Timber.d(
                  "Being asked if $name@${this.hashCode()} idle state: $it current value is ${this.value} threshold is $threshold")
            }
          }

  private var value = AtomicInteger(0)

  override fun increment() {
    value.incrementAndGet().also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
  }

  override fun set(value: Int) {
    if (enableLog) {
      Timber.d("Setting $name value to $value threshold is $threshold")
    }
    this.value.set(value).also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
  }

  override fun decrement() {
    value.decrementAndGet().also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
  }

  override var threshold: Int = 0
    set(value) = run {
      if (enableLog) {
        Timber.d("Setting $name threshold to $value current value is ${this.value.get()}")
      }
      field = value.also { if (isIdleNow) resourceCallback?.onTransitionToIdle() }
    }
}
