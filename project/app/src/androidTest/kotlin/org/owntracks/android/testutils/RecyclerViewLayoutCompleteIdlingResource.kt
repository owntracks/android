package org.owntracks.android.testutils

import androidx.test.espresso.IdlingResource
import org.owntracks.android.ui.base.RecyclerViewLayoutCompleteListener
import timber.log.Timber

class RecyclerViewLayoutCompleteIdlingResource(
    private val idlingCallback: RecyclerViewLayoutCompleteListener.RecyclerViewIdlingCallback
) : IdlingResource, RecyclerViewLayoutCompleteListener {
  private var resourceCallback: IdlingResource.ResourceCallback? = null

  init {
    idlingCallback.setRecyclerViewLayoutCompleteListener(this)
  }

  override fun getName(): String = "RecyclerViewLayoutCompleteIdlingResource"

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.resourceCallback = callback
  }

  override fun isIdleNow(): Boolean = idlingCallback.isRecyclerViewLayoutCompleted

  override fun onLayoutCompleted() {
    Timber.d("Layout complete!")
    resourceCallback?.run {
      idlingCallback.removeRecyclerViewLayoutCompleteListener(
          this@RecyclerViewLayoutCompleteIdlingResource)
      Timber.d("Transition to idle")
      onTransitionToIdle()
    }
  }

  fun setUnidle() {
    idlingCallback.isRecyclerViewLayoutCompleted = false
  }
}
