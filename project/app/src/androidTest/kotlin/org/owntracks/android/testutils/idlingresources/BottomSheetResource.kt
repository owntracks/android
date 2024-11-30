package org.owntracks.android.testutils.idlingresources

import android.view.View
import androidx.test.espresso.IdlingResource
import com.google.android.material.bottomsheet.BottomSheetBehavior

abstract class BottomSheetResource(private val bottomSheetBehavior: BottomSheetBehavior<View>) :
    IdlingResource, BottomSheetBehavior.BottomSheetCallback() {

  private var isIdle: Boolean = false
  private var resourceCallback: IdlingResource.ResourceCallback? = null

  override fun onSlide(bottomSheet: View, slideOffset: Float) {}

  override fun onStateChanged(bottomSheet: View, newState: Int) {
    val wasIdle = isIdle
    isIdle = isDesiredState(newState)
    if (!wasIdle && isIdle) {
      bottomSheetBehavior.removeBottomSheetCallback(this)
      resourceCallback?.onTransitionToIdle()
    }
  }

  override fun isIdleNow(): Boolean {
    return isIdle
  }

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
    resourceCallback = callback

    val state = bottomSheetBehavior.state
    isIdle = isDesiredState(state)
    if (isIdle) {
      resourceCallback!!.onTransitionToIdle()
    } else {
      bottomSheetBehavior.addBottomSheetCallback(this)
    }
  }

  abstract fun isDesiredState(@BottomSheetBehavior.State state: Int): Boolean

  class BottomSheetStateResource(
      bottomSheetBehavior: BottomSheetBehavior<View>,
      @BottomSheetBehavior.State private val desiredState: Int
  ) : BottomSheetResource(bottomSheetBehavior) {

    override fun getName(): String {
      return "BottomSheet awaiting state: $desiredState"
    }

    override fun isDesiredState(@BottomSheetBehavior.State state: Int): Boolean {
      return state == desiredState
    }
  }
}
