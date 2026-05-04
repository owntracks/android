package org.owntracks.android.ui.welcome.fragments

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PlayFragmentViewModel @Inject constructor() : ViewModel() {
  fun setPlayServicesAvailable(message: String) {
    mutableMessage.value = message
    mutablePlayServicesFixAvailable.value = false
  }

  fun setPlayServicesNotAvailable(fixAvailable: Boolean, message: String) {
    mutableMessage.value = message
    mutablePlayServicesFixAvailable.value = fixAvailable
  }

  private val mutableMessage = MutableStateFlow("")
  val message = mutableMessage.asStateFlow()

  private val mutablePlayServicesFixAvailable = MutableStateFlow(false)
  val playServicesFixAvailable = mutablePlayServicesFixAvailable.asStateFlow()
}
