package org.owntracks.android.ui.welcome

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.owntracks.android.preferences.Preferences

@HiltViewModel
class WelcomeViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {
  private val mutableCurrentFragmentPosition = MutableStateFlow(0)
  val currentFragmentPosition = mutableCurrentFragmentPosition.asStateFlow()

  private val mutableNextEnabled = MutableStateFlow(true)
  val nextEnabled = mutableNextEnabled.asStateFlow()

  private val mutableDoneEnabled = MutableStateFlow(false)
  val doneEnabled = mutableDoneEnabled.asStateFlow()

  fun moveToPage(position: Int) {
    mutableCurrentFragmentPosition.value = position
  }

  fun nextPage() {
    mutableNextEnabled.value = false
    mutableCurrentFragmentPosition.update { it + 1 }
  }

  fun previousPage() {
    mutableCurrentFragmentPosition.update { it - 1 }
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
