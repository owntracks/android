package org.owntracks.android.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.support.Preferences
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel() {
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

    fun setWelcomeCanProceed() {
        mutableNextEnabled.postValue(true)
        mutableDoneEnabled.postValue(false)
    }

    fun setWelcomeCannotProceed() {
        mutableNextEnabled.postValue(true)
        mutableDoneEnabled.postValue(false)
    }

    fun setWelcomeIsAtEnd() {
        preferences.setSetupCompleted()
        mutableNextEnabled.postValue(false)
        mutableDoneEnabled.postValue(true)
    }
}