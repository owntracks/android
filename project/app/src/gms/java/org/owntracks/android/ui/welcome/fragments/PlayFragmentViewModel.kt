package org.owntracks.android.ui.welcome.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayFragmentViewModel @Inject constructor() : ViewModel() {
  fun setPlayServicesAvailable(message: String) {
    mutableMessage.postValue(message)
    mutablePlayServicesFixAvailable.postValue(false)
  }

  fun setPlayServicesNotAvailable(fixAvailable: Boolean, message: String) {
    mutableMessage.postValue(message)
    mutablePlayServicesFixAvailable.postValue(fixAvailable)
  }

  private val mutableMessage = MutableLiveData<String>()
  val message: LiveData<String> = mutableMessage

  private val mutablePlayServicesFixAvailable = MutableLiveData(false)
  val playServicesFixAvailable: LiveData<Boolean> = mutablePlayServicesFixAvailable
}
