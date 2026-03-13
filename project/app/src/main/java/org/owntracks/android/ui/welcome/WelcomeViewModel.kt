package org.owntracks.android.ui.welcome

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.preferences.Preferences

@HiltViewModel
class WelcomeViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {
  private val mutableCurrentFragmentPosition = MutableStateFlow(0)
  val currentFragmentPosition: StateFlow<Int> = mutableCurrentFragmentPosition

  private val mutableNextEnabled = MutableStateFlow(true)
  val nextEnabled: StateFlow<Boolean> = mutableNextEnabled

  private val mutableDoneEnabled = MutableStateFlow(false)
  val doneEnabled: StateFlow<Boolean> = mutableDoneEnabled

  fun moveToPage(position: Int) {
    mutableCurrentFragmentPosition.value = position
  }

  fun nextPage() {
    mutableNextEnabled.value = false
    moveToPage(currentFragmentPosition.value + 1)
  }

  fun previousPage() {
    moveToPage(currentFragmentPosition.value - 1)
  }

  fun setWelcomeState(progressState: ProgressState) {
    when (progressState) {
      ProgressState.PERMITTED -> {
        mutableNextEnabled.value = true
        mutableDoneEnabled.value = false
      }
      ProgressState.NOT_PERMITTED -> {
        mutableNextEnabled.value = false
        mutableDoneEnabled.value = false
      }
      ProgressState.FINISHED -> {
        preferences.setupCompleted = true
        mutableNextEnabled.value = false
        mutableDoneEnabled.value = true
      }
    }
  }

  enum class ProgressState {
    PERMITTED,
    NOT_PERMITTED,
    FINISHED
  }
}
