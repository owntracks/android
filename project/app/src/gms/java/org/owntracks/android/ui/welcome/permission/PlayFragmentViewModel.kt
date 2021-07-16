package org.owntracks.android.ui.welcome.permission

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayFragmentViewModel @Inject constructor() : ViewModel() {
    val fixAvailable = MutableLiveData(true)
    val message = MutableLiveData("")
}