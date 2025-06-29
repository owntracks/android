package org.owntracks.android.testutils

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/**
 * Bottom sheet set state action - from
 * https://eng.wealthfront.com/2020/12/21/espresso-friendly-bottom-sheet-interactions/
 *
 * @property desiredState
 * @constructor Create empty Bottom sheet set state action
 */
class BottomSheetSetStateAction(@BottomSheetBehavior.State private val desiredState: Int) :
    ViewAction {

  override fun getConstraints(): Matcher<View> {
    return Matchers.any(View::class.java)
  }

  override fun perform(uiController: UiController, view: View) {
    val bottomSheetBehavior = BottomSheetBehavior.from(view)
    bottomSheetBehavior.state = desiredState
  }

  override fun getDescription(): String = "Set BottomSheet to state: $desiredState"
}
