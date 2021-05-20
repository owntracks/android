package org.owntracks.android.ui.welcome.permission

import androidx.databinding.Bindable
import dagger.hilt.android.scopes.ActivityScoped
import org.owntracks.android.BR
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@ActivityScoped
class PlayFragmentViewModel @Inject constructor() : BaseViewModel<PlayFragmentMvvm.View?>() {
    @get:Bindable
    var fixAvailable = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.fixAvailable)
        }

    @get:Bindable
    var message: String? = null
        set(value) {
            field = value
            notifyPropertyChanged(BR.message)
        }

    fun onFixClicked() {
        view!!.requestFix()
    }
}