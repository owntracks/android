package org.owntracks.android.ui.welcome.permission

import androidx.databinding.Bindable
import org.owntracks.android.BR
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@PerActivity
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