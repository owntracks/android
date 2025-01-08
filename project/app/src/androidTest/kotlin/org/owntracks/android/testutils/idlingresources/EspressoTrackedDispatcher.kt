package org.owntracks.android.testutils.idlingresources

import androidx.test.espresso.idling.CountingIdlingResource
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * A kotlin coroutine dispatcher that wraps an existing [CoroutineDispatcher] with a
 * [CountingIdlingResource] to track completion, and therefore makes it useful in an espresso
 * context. From https://gist.github.com/jonreeve/6c6ea2cc5893c87cd0dabfb5d3d14eb3
 *
 * @property delegateDispatcher dispatcher to wrap
 * @constructor Create empty Espresso tracked dispatcher
 */
class EspressoTrackedDispatcher(private val delegateDispatcher: CoroutineDispatcher) :
    CoroutineDispatcher(), DispatcherWithIdlingResource {
  override val idlingResource: CountingIdlingResource =
      CountingIdlingResource("EspressoTrackedDispatcher for $delegateDispatcher")

  override fun dispatch(context: CoroutineContext, block: Runnable) =
      delegateDispatchWithCounting(delegateDispatcher, context, block, idlingResource)

  private fun delegateDispatchWithCounting(
      delegateDispatcher: CoroutineDispatcher,
      context: CoroutineContext,
      block: Runnable,
      idlingResource: CountingIdlingResource
  ) {
    idlingResource.increment()
    delegateDispatcher.dispatch(context) {
      try {
        block.run()
      } finally {
        idlingResource.decrement()
      }
    }
  }
}
