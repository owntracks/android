package org.owntracks.android.ui.welcome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.support.Preferences
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {
    var currentFragmentPosition: MutableLiveData<Int> = MutableLiveData(0)

    val doneEnabled = MutableLiveData(false)
    val nextEnabled = MutableLiveData(false)

    fun moveNext() {
        nextEnabled.postValue(false)
        currentFragmentPosition.postValue(currentFragmentPosition.value!! + 1)
    }

    fun moveBack() {
        nextEnabled.postValue(false)
        currentFragmentPosition.postValue(currentFragmentPosition.value!! - 1)
    }

    fun setSetupCompleted() {
        preferences.setSetupCompleted()
    }
}