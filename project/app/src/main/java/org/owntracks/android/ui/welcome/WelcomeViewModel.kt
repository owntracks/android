package org.owntracks.android.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences

@HiltViewModel
class WelcomeViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {
  private val mutableCurrentFragmentPosition: MutableLiveData<Int> = MutableLiveData(0)
  val currentFragmentPosition: LiveData<Int> = mutableCurrentFragmentPosition

  private val mutableNextEnabled = MutableLiveData(true)
  val nextEnabled: LiveData<Boolean> = mutableNextEnabled

  private val mutableDoneEnabled = MutableLiveData(false)
  val doneEnabled: LiveData<Boolean> = mutableDoneEnabled

  fun moveToPage(position: Int) {
    mutableCurrentFragmentPosition.postValue(position)
  }

  fun nextPage() {
    mutableNextEnabled.postValue(false)
    moveToPage((currentFragmentPosition.value?.plus(1)) ?: 0)
  }

  fun previousPage() {
    moveToPage((currentFragmentPosition.value?.minus(1)) ?: 0)
  }

  fun setWelcomeState(progressState: ProgressState) {
    when (progressState) {
      ProgressState.PERMITTED -> {
        mutableNextEnabled.postValue(true)
        mutableDoneEnabled.postValue(false)
      }
      ProgressState.NOT_PERMITTED -> {
        mutableNextEnabled.postValue(false)
        mutableDoneEnabled.postValue(false)
      }
      ProgressState.FINISHED -> {
        preferences.setupCompleted = true
        mutableNextEnabled.postValue(false)
        mutableDoneEnabled.postValue(true)
      }
    }
  }

  enum class ProgressState {
    PERMITTED,
    NOT_PERMITTED,
    FINISHED
  }
}
